package com.webjob.application.mapper;

import com.webjob.application.dto.Response.AdminResumeResponse;
import com.webjob.application.models.Entity.UserResume;
import org.springframework.stereotype.Component;

@Component
public class UserResumeMapper {

   public AdminResumeResponse toAdminResumeResponse(AdminResumeResponse userResume){
       if(userResume==null){
           return null;
       }
       return AdminResumeResponse.builder()
               .id(userResume.getId())
               .resumeName(userResume.getResumeName())
               .ownerEmail(userResume.getOwnerEmail())
               .ownerName(userResume.getOwnerName())
               .isDefault(userResume.getIsDefault())
               .createdAt(userResume.getCreatedAt())
               .url(userResume.getUrl())
               .totalApplications(userResume.getTotalApplications())
               .build();

   }
}
