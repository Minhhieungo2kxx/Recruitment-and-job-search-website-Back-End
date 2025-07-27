package com.webjob.application.Services.UploadFileServer;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.webjob.application.Configs.UploadfileServer.UploadFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {
    @Autowired
    private Cloudinary cloudinary;
    @Autowired
    private UploadFile uploadFile;


    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        uploadFile.vadidateUploadFile(file,folderName);
        String originalName = file.getOriginalFilename();
        String baseName = originalName.substring(0, originalName.lastIndexOf("."));
        String uniqueName =System.currentTimeMillis()+"-"+baseName;
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folderName,
                "public_id",uniqueName,
                "access_mode", "public" // Chỉ định quyền truy cập công khai
        ));

        return result.get("secure_url").toString(); // Trả về link trực tiếp tới file trên Cloudinary
    }
}
