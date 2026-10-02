package com.webjob.application.service;

import com.webjob.application.dto.Request.UpdateResumeNameRequest;
import com.webjob.application.dto.Request.UploadResumeRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.dto.record.RecommendationCacheInvalidateEvent;
import com.webjob.application.dto.event.ResumeFileDeletedEvent;
import com.webjob.application.exception.Customs.AppException;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.mapper.UserResumeMapper;
import com.webjob.application.models.Entity.JobRecommendation;
import com.webjob.application.models.Entity.TemporaryUpload;
import com.webjob.application.models.Entity.User;
import com.webjob.application.models.Entity.UserResume;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.repository.JobRecommendationRepository;
import com.webjob.application.repository.TemporaryUploadRepository;
import com.webjob.application.repository.UserResumeRepository;
import com.webjob.application.component.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserResumeService {
    private final UserResumeRepository userResumeRepository;
    private final ApplicationMapper applicationMapper;
    private final SecurityUtils securityUtils;
    private final UserResumeMapper userResumeMapper;
    private final ModelMapper modelMapper;
    private final TemporaryUploadRepository temporaryUploadRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationRepository applicationRepository;
    private final JobRecommendationRepository jobRecommendationRepository;


    public List<UserResumeResponse> getMyResumes() {

        Long userId = securityUtils.getCurrentUserId();

        return userResumeRepository
                .findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(applicationMapper::toUserResumeResponse)
                .toList();
    }

    public ResponseDTO<List<UserResumeResponse>> getMyResumesPageList(int page, int size) {
        // Bỏ hoàn toàn try-catch, chỉ giữ lại logic kiểm tra số âm/bằng 0
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Long userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserResume> pagelist = userResumeRepository.findByUserId(userId, pageable);

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);

        List<UserResumeResponse> list = pagelist.getContent().stream()
                .map(applicationMapper::toUserResumeResponse)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);

    }

    public ResponseDTO<List<AdminResumeResponse>> getAllCvAdmin(int page, int size) {
        // Bỏ hoàn toàn try-catch, chỉ giữ lại logic kiểm tra số âm/bằng 0
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminResumeResponse> pagelist = userResumeRepository.getAllResumeForAdmin(pageable);
        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);

        List<AdminResumeResponse> list = pagelist.getContent().stream()
                .map(userResumeMapper::toAdminResumeResponse)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);

    }

    public UserResumeResponse getById(Long id) {
        UserResume userResume = userResumeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("UserResume not found"));

        return modelMapper.map(userResume, UserResumeResponse.class);
    }

    @Transactional
    public UserResumeResponse createResume(UploadResumeRequest request) {
        User user = securityUtils.getCurrentUser();
        TemporaryUpload upload = temporaryUploadRepository.findByPublicIdAndUsedFalse(request.getPublicId())
                .orElseThrow(() -> new AppException("File không tồn tại."));
        if (!upload.getUser().getId().equals(user.getId())) {
            throw new AppException("CV không thuộc người dùng.");
        }
        long count = userResumeRepository.countByUserId(user.getId());
        if (count >= 5) {
            throw new BadRequestException("Bạn chỉ được lưu tối đa 5 CV.");
        }
        boolean isDefault = count == 0 || Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault) {
            userResumeRepository.clearDefaultResume(user.getId());
        }

        UserResume resume = UserResume.builder()
                .name("CV " + LocalDate.now())
                .url(upload.getUrl())
                .publicId(upload.getPublicId())
                .resourceType(upload.getResourceType())
                .isDefault(isDefault)
                .user(user).build();

        UserResume savedResume = userResumeRepository.save(resume);
        upload.setUsed(true);
        temporaryUploadRepository.save(upload);
        if (isDefault) {
            publishRecommendationCacheInvalidateEvent(user.getId(), "CV mặc định thay đổi");
        }
        return modelMapper.map(savedResume, UserResumeResponse.class);
    }

    public void publishRecommendationCacheInvalidateEvent(Long userId, String message) {
        eventPublisher.publishEvent(new RecommendationCacheInvalidateEvent(userId, message));
    }


    @Transactional
    public UserResumeResponse updateResumeName(Long id, UpdateResumeNameRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        UserResume resume = userResumeRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV."));

        resume.setName(request.getName().trim());

        UserResume savedResume = userResumeRepository.save(resume);

        return modelMapper.map(savedResume, UserResumeResponse.class);
    }
    @Transactional
    public UserResumeResponse setDefaultResume(Long id) {
        User currentUser = securityUtils.getCurrentUser();

        UserResume resume = userResumeRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV."));

        // Đã là default thì không cần xử lý gì
        if (Boolean.TRUE.equals(resume.getIsDefault())) {
            return modelMapper.map(resume, UserResumeResponse.class);
        }

        // Bỏ default của CV hiện tại
        userResumeRepository.clearDefaultResume(currentUser.getId());

        // Set CV này thành default
        resume.setIsDefault(true);

        UserResume savedResume = userResumeRepository.save(resume);

        // Chỉ invalidate cache khi default thực sự thay đổi
        publishRecommendationCacheInvalidateEvent(
                currentUser.getId(),
                "CV mặc định thay đổi"
        );

        return modelMapper.map(savedResume, UserResumeResponse.class);
    }

    @Transactional
    public void deleteMyResume(Long resumeId) {


        UserResume resume = userResumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume không tồn tại"));
        // Resume phải thuộc chính User
        if (!resume.getUser().getId().equals(securityUtils.getCurrentUserId())) {
            throw new ForbiddenException("Bạn không có quyền xóa CV này.");
        }

        deleteResumeInternal(resume);
    }

    @Transactional
    public void deleteResumeByAdmin(Long resumeId) {

        UserResume resume = userResumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume không tồn tại"));

        deleteResumeInternal(resume);
    }

    private void deleteResumeInternal(UserResume resume) {

        if (applicationRepository.existsByResumeId(resume.getId())) {
            throw new BadRequestException("CV đang được sử dụng để ứng tuyển.");
        }

        Long userId = resume.getUser().getId();
        boolean wasDefault = Boolean.TRUE.equals(resume.getIsDefault());

        String publicId = resume.getPublicId();
        String resourceType = resume.getResourceType();

        TemporaryUpload temporaryUpload =
                temporaryUploadRepository.findByPublicId(publicId).orElse(null);

        // Xóa các JobRecommendation liên quan (JPA sẽ tự động xóa các JobRecommendationItem nhờ CascadeType.ALL)
        List<JobRecommendation> recommendations = jobRecommendationRepository.findByResumeId(resume.getId());
        if (!recommendations.isEmpty()) {
            jobRecommendationRepository.deleteAll(recommendations);
        }


        // Xóa CV
        userResumeRepository.delete(resume);

        // Xóa temporary upload
        if (temporaryUpload != null) {
            temporaryUploadRepository.delete(temporaryUpload);
        }

        //Nếu CV vừa xóa là default → chọn CV mới nhất làm default
        if (wasDefault) {
            userResumeRepository
                    .findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                    .ifPresent(nextDefault -> {
                        nextDefault.setIsDefault(true);
                        userResumeRepository.save(nextDefault);
                    });

            //  Dù có CV mới hay không, recommendation cache đều phải invalidate
            publishRecommendationCacheInvalidateEvent(userId, "CV mặc định thay đổi");

        }

        // Event xóa file trên storage
        eventPublisher.publishEvent(
                new ResumeFileDeletedEvent(publicId, resourceType)
        );
    }


}
