 package com.xuecheng.media.service.impl;

 import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
 import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
 import com.j256.simplemagic.ContentInfo;
 import com.j256.simplemagic.ContentInfoUtil;
 import com.xuecheng.base.exception.XueChengPlusException;
 import com.xuecheng.base.model.PageParams;
 import com.xuecheng.base.model.PageResult;
 import com.xuecheng.base.model.RestResponse;
 import com.xuecheng.media.mapper.MediaFilesMapper;
 import com.xuecheng.media.mapper.MediaProcessMapper;
 import com.xuecheng.media.model.dto.QueryMediaParamsDto;
 import com.xuecheng.media.model.dto.UploadFileParamsDto;
 import com.xuecheng.media.model.dto.UploadFileResultDto;
 import com.xuecheng.media.model.po.MediaFiles;
 import com.xuecheng.media.model.po.MediaProcess;
 import com.xuecheng.media.service.MediaFileService;
 import io.minio.*;
 import io.minio.messages.DeleteError;
 import io.minio.messages.DeleteObject;
 import lombok.extern.slf4j.Slf4j;
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.springframework.beans.BeanUtils;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.http.MediaType;
 import org.springframework.stereotype.Service;
 import org.apache.commons.codec.digest.DigestUtils;
 import org.springframework.transaction.annotation.Transactional;

 import java.io.*;
 import java.text.SimpleDateFormat;
 import java.time.LocalDateTime;
 import java.util.Date;
 import java.util.List;
 import java.util.stream.Collectors;
 import java.util.stream.Stream;

 /**
  * @description TODO
  * @author Mr.M
  * @date 2022/9/10 8:58
  * @version 1.0
  */
 @Slf4j
  @Service
 public class MediaFileServiceImpl implements MediaFileService {

   @Autowired
   MediaFilesMapper mediaFilesMapper;
   @Autowired
  MinioClient minioClient;
   @Autowired
  MediaProcessMapper mediaProcessMapper;
 @Value("${minio.bucket.files}")
  private String bucket_mediafiles;

  @Value("${minio.bucket.videofiles}")
  private String bucket_video;

  @Autowired
  MediaFileServiceImpl currentProxy;
  @Override
  public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

   //构建查询条件对象
   LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

   //分页对象
   Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
   // 查询数据内容获得结果
   Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
   // 获取数据列表
   List<MediaFiles> list = pageResult.getRecords();
   // 获取数据总数
   long total = pageResult.getTotal();
   // 构建结果集
   PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
   return mediaListResult;

  }
  private String getMimeType(String extension){
   if(extension == null){
    extension = "";
   }
   //根据扩展名取出mimeType
   ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
   String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
   if(extensionMatch!=null){
    mimeType = extensionMatch.getMimeType();
   }
   return mimeType;

  }
  @Override
  public boolean addMediaFileToMinIO(String localFilePath, String mimiType, String bucket, String objectName){
   try {
    UploadObjectArgs builder = UploadObjectArgs.builder().
            bucket(bucket).
            filename(localFilePath).object(objectName).contentType(mimiType).build();
    minioClient.uploadObject(builder);
    log.debug("上传文件成功,bucket:{},objectName:{}",bucket,objectName);
    return true;
   } catch (Exception e) {
      e.printStackTrace();
      log.info("上传文件出错,bucket:{},objectName:{}，错误信息:{}",bucket,objectName,e.getMessage());

    }
   return false;
  }

  private String getDefaultFolderPath(){
   SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
   String s = simpleDateFormat.format(new Date()).replace("-", "/") + "/";
   return s;
  }

  private String getFileMd5(File file){
   try(FileInputStream fileInputStream =new FileInputStream(file)){
    String s = DigestUtils.md5Hex(fileInputStream);
    return s;
   }catch (Exception e){
     e.printStackTrace();
     return null;
   }


  }


  @Override
  public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto,
                                        String localFilePath,
                                        String objectName) {
   String filename = uploadFileParamsDto.getFilename();
   String ext = filename.substring(filename.lastIndexOf("."));
   String defaultFolderPath = getDefaultFolderPath();
   String mineType = getMimeType(ext);
   String fileMd5 = getFileMd5(new File(localFilePath));
   if(StringUtils.isEmpty(objectName)){
    objectName = defaultFolderPath + fileMd5 + ext;
   }
   boolean b = addMediaFileToMinIO(localFilePath, mineType, bucket_mediafiles, objectName);
   if(b==false){
    XueChengPlusException.cast("上传文件失败");
   }
   MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, uploadFileParamsDto, bucket_mediafiles, objectName, fileMd5);
   if(mediaFiles==null){
    XueChengPlusException.cast("文件上传保存信息失败");
   }
   UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
   BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
   return uploadFileResultDto;
  }
  @Transactional
  @Override
  public MediaFiles addMediaFilesToDb(Long companyId, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName, String fileMd5) {
   MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
   if(mediaFiles==null){
    mediaFiles= new MediaFiles();
    BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
    mediaFiles.setId(fileMd5);
    mediaFiles.setCompanyId(companyId);
    mediaFiles.setBucket(bucket);
    mediaFiles.setFilePath(objectName);
    mediaFiles.setFileId(fileMd5);
    mediaFiles.setUrl("/"+bucket+"/"+objectName);
    mediaFiles.setCreateDate(LocalDateTime.now());
    mediaFiles.setStatus("1");
    mediaFiles.setAuditStatus("002003");
    int insert = mediaFilesMapper.insert(mediaFiles);
    if(insert<=0){
     log.error("文件保存至数据库出错,bucket:{},objectName:{}",bucket,objectName);
     return null;
    }
    //转码
    addWaitingTask(mediaFiles);


    return mediaFiles;
   }
   return mediaFiles;
  }

  /**
   * 添加待处理任务
   * @param mediaFiles 媒资文件信息
   */
  private void addWaitingTask(MediaFiles mediaFiles) {
   String filename = mediaFiles.getFilename();
   String ext = filename.substring(filename.lastIndexOf("."));
   String mineType = getMimeType(ext);
   if(mineType.equals("video/x-msvideo")){
    MediaProcess mediaProcess = new MediaProcess();

    BeanUtils.copyProperties(mediaFiles,mediaProcess);
    mediaProcess.setStatus("1");
    mediaProcess.setCreateDate(LocalDateTime.now());
    mediaProcess.setFailCount(0);
    mediaProcess.setUrl(null);
    int insert = mediaProcessMapper.insert(mediaProcess);
   }

  }


   @Override
  public RestResponse<Boolean> checkFile(String fileMd5) {
   MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
   if(mediaFiles!=null){
    String bucket = mediaFiles.getBucket();
    String filePath = mediaFiles.getFilePath();

    GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucket).object(filePath).build();
    try {
    FilterInputStream filterInputStream = minioClient.getObject(objectArgs);
      if (filterInputStream!=null){
       return RestResponse.success(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
   }
   return RestResponse.success(false);
  }

  @Override
  public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
   String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

   GetObjectArgs objectArgs = GetObjectArgs.builder()
           .bucket(bucket_video).object(chunkFileFolderPath+chunkIndex).build();
   try {
    FilterInputStream filterInputStream = minioClient.getObject(objectArgs);
    if (filterInputStream!=null){
     return RestResponse.success(true);
    }
   } catch (Exception e) {
    e.printStackTrace();
   }

   return RestResponse.success(false);
  }

  @Override
  public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
   String chunkFileFolderPath = getChunkFileFolderPath(fileMd5)+chunk;
   String mineType = getMimeType(null);
   boolean b = addMediaFileToMinIO(localChunkFilePath, mineType, bucket_video, chunkFileFolderPath);
   if(!b){
    return  RestResponse.validfail(false,"上传文件失败");
   }
    return RestResponse.success(true);
  }

  @Override
  public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
   String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
   List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i -> ComposeSource.builder()
           .bucket(bucket_video).object(chunkFileFolderPath + i).build()).collect(Collectors.toList());
   String filename = uploadFileParamsDto.getFilename();
   String ext = filename.substring(filename.lastIndexOf("."));
   String name = getFilePathByMd5(fileMd5, ext);

   ComposeObjectArgs build = ComposeObjectArgs.builder().bucket(bucket_video).object(name).sources(sources).build();

   try {
    minioClient.composeObject(build);
   } catch (Exception e) {
    e.printStackTrace();
    log.error("合并文件出错,bucket:{},objectName:{},错误信息:{}",bucket_video,name,e.getMessage());
    return RestResponse.validfail(false,"合并文件异常");
   }

   File file = downloadFileFromMinIO(bucket_video, name);
   try(FileInputStream fileInputStream = new FileInputStream(file)) {
    String mergedMd5 = DigestUtils.md5Hex(fileInputStream);
    if(!fileMd5.equals(mergedMd5)){
     log.error("校验文件Md5不一致，原始文件：{}，合并文件：{}",fileMd5,mergedMd5);
     return RestResponse.validfail(false,"文件校验失败");
    }
    uploadFileParamsDto.setFileSize(file.length());
   }  catch (Exception e) {
    return RestResponse.validfail(false,"文件校验失败");
   }
   MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, uploadFileParamsDto, bucket_video, name, fileMd5);
   if(mediaFiles==null){
    return RestResponse.validfail(false,"文件入库失败");
   }

   String chunkFileFolderPath1 = getChunkFileFolderPath(fileMd5);
   clearChunkFiles(chunkFileFolderPath1,chunkTotal);



   return RestResponse.success(true);
  }

  private void clearChunkFiles(String filePath,int chunkTotal){
   Iterable<DeleteObject> objects =Stream.iterate(0, i -> ++i)
           .limit(chunkTotal).map(i -> new
                   DeleteObject(filePath+i))
           .collect(Collectors.toList());
   RemoveObjectsArgs build = RemoveObjectsArgs.builder().bucket(bucket_video).objects(objects).build();
   Iterable<Result<DeleteError>> results = minioClient.removeObjects(build);
   results.forEach(f->{
    try {
     DeleteError deleteError = f.get();
    } catch (Exception e) {
      e.printStackTrace();
    }

   });
  }



  //根据桶和文件路径从minio下载文件
  @Override
  public File downloadFileFromMinIO(String bucket, String objectName){
   //临时文件
   File minioFile = null;
   FileOutputStream outputStream = null;
   try{
    InputStream stream = minioClient.getObject(GetObjectArgs.builder()
            .bucket(bucket)
            .object(objectName)
            .build());
    //创建临时文件
    minioFile=File.createTempFile("minio", ".merge");
    outputStream = new FileOutputStream(minioFile);
    IOUtils.copy(stream,outputStream);
    return minioFile;
   } catch (Exception e) {
    e.printStackTrace();
   }finally {
    if(outputStream!=null){
     try {
      outputStream.close();
     } catch (IOException e) {
      e.printStackTrace();
     }
    }
   }
   return null;
  }

     @Override
  public MediaFiles getFilesById(String mediaId) {
      MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
      return mediaFiles;
     }


     private String getFilePathByMd5(String fileMd5,String fileExt) {
   return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 +fileExt;
  }
  private String getChunkFileFolderPath(String fileMd5) {
   return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
  }

 }
