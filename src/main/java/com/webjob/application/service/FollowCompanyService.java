package com.webjob.application.service;

import com.webjob.application.dto.Response.FollowCompanyResponse;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SavedJobResponse;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.FollowCompany;
import com.webjob.application.models.Entity.SavedJob;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.FollowCompanyRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor

public class FollowCompanyService {
    private final FollowCompanyRepository followRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public void followCompany(Long companyId) {

        User user=securityUtils.getCurrentUser();

        if (followRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new BadRequestException("FollowCompany already saved.");
        }

        Company company = companyRepository.findByIdAndDeletedFalse(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        FollowCompany follow = new FollowCompany();
        follow.setUser(user);
        follow.setCompany(company);
        follow.setNotificationEnabled(true);

        followRepository.save(follow);
    }
    @Transactional
    public void unfollowCompany(Long companyId) {

        Long userId = securityUtils.getCurrentUserId();
        FollowCompany followCompany=followRepository.findByUserIdAndCompanyId(userId,companyId)
                .orElseThrow( () -> new ResourceNotFoundException("FollowCompany not found"));

        followRepository.delete(followCompany);
    }
    @Transactional(readOnly = true)
    public ResponseDTO<List<FollowCompanyResponse>> getMyFollowCompanies(int page, int size) {

        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Long userId=securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page-1, size, Sort.by("followedAt").descending());
        Page<FollowCompany> pagelist =followRepository.findByUserId(userId,pageable);

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);

        List<FollowCompanyResponse> list = pagelist.getContent().stream()
                .map(this::toResponse)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);



    }
    public FollowCompanyResponse toResponse(FollowCompany entity) {
        if (entity == null) {
            return null;
        }

        FollowCompanyResponse response = new FollowCompanyResponse();

        if (entity.getCompany() != null) {
            response.setCompanyId(entity.getCompany().getId());
            response.setCompanyName(entity.getCompany().getName());
            response.setLogo(entity.getCompany().getLogo());
        }
        response.setNotificationEnabled(entity.isNotificationEnabled());
        response.setFollowedAt(entity.getFollowedAt());

        return response;
    }
    @Transactional
    public void enableNotification(Long id) {
        Long userID=securityUtils.getCurrentUserId();
        FollowCompany follow =followRepository.findByIdAndUserId(id,userID)
                .orElseThrow(() -> new BadRequestException("Follow company not found"));
        if (follow.isNotificationEnabled()) {
            throw new BadRequestException("Notification is already enabled");
        }
        follow.setNotificationEnabled(true);
        followRepository.save(follow);
    }

    @Transactional
    public void disableNotification(Long id) {
        Long userID=securityUtils.getCurrentUserId();
        FollowCompany follow =followRepository.findByIdAndUserId(id,userID)
                .orElseThrow(() -> new BadRequestException("Follow company not found"));
        if (!follow.isNotificationEnabled()) {
            throw new BadRequestException("Notification is already disabled");
        }
        follow.setNotificationEnabled(true);
        followRepository.save(follow);
    }
}
