package com.webjob.application.service;


import com.webjob.application.dto.Request.Search.SubscriberFilterRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.exception.Customs.ConflictException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.SubscriberMapper;
import com.webjob.application.messaging.producer.EmailProducer;
import com.webjob.application.models.Entity.*;
import com.webjob.application.dto.Request.SubscriberRequest;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.repository.SubscriberRepository;


import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.service.SendEmail.EmailService;
import com.webjob.application.service.Specification.ApplicationSpecification;
import com.webjob.application.service.Specification.SubscriberSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    private final SkillRepository skillRepository;

    private final ModelMapper modelMapper;
    private final SubscriberMapper subscriberMapper;

    private final SecurityUtils securityUtils;


    @Transactional
    public SubscriberResponse createSubscriber(SubscriberRequest request) {

        Subscriber subscriber = new Subscriber();
        modelMapper.map(request,subscriber);

        Map<Long, Skill> skillMap = getSkillMap(request.getSkillIds());

        if (skillMap.size() != request.getSkillIds().size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều Skill không tồn tại.");
        }

        List<SubscriberSkill> subscriberSkills =
                request.getSkillIds()
                        .stream()
                        .map(id -> {
                            SubscriberSkill ss = new SubscriberSkill();
                            ss.setSubscriber(subscriber);
                            ss.setSkill(skillMap.get(id));
                            return ss;
                        })
                        .toList();
        subscriber.setSubscriberSkills(subscriberSkills);
        subscriber.setUser(securityUtils.getCurrentUser());
        Subscriber saved = subscriberRepository.save(subscriber);
        return subscriberMapper.mapToResponse(saved);
    }


    @Transactional
    public SubscriberResponse updateSubscriber(Long id,SubscriberRequest request) {


        Subscriber subscriber = getById(id);

        if(!subscriber.getUser().getId().equals(securityUtils.getCurrentUserId())){
            throw new ForbiddenException("You dont permission !");
        }

        modelMapper.map(request,subscriber);

        Map<Long, Skill> skillMap = getSkillMap(request.getSkillIds());

        if (skillMap.size() != request.getSkillIds().size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều Skill không tồn tại.");
        }

        subscriber.getSubscriberSkills().clear();

        subscriberRepository.flush();
        List<SubscriberSkill> subscriberSkills = request.getSkillIds()
                .stream()
                .map(skillId -> {

                    SubscriberSkill ss = new SubscriberSkill();

                    ss.setSubscriber(subscriber);
                    ss.setSkill(skillMap.get(skillId));

                    return ss;
                })
                .toList();
        subscriber.getSubscriberSkills().addAll(subscriberSkills);

        Subscriber saved = subscriberRepository.save(subscriber);
        return subscriberMapper.mapToResponse(saved);
    }




    private Map<Long, Skill> getSkillMap(List<Long> skillIds) {

        if (skillIds == null || skillIds.isEmpty()) {
            return Map.of();
        }

        List<Skill> skills = skillRepository.findAllById(skillIds);

        return skills.stream()
                .collect(Collectors.toMap(
                        Skill::getId,
                        Function.identity()
                ));
    }


    public Subscriber getById(Long id) {
        return subscriberRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));
    }


    @Transactional
    public void deleteSubscriber(Long id){

        Subscriber subscriber = getById(id);
        if(!subscriber.getUser().getId().equals(securityUtils.getCurrentUserId())){
            throw new ForbiddenException("You dont permission !");
        }

        subscriber.getSubscriberSkills().clear();

        subscriberRepository.flush();

        subscriberRepository.delete(subscriber);
    }
    @Transactional(readOnly = true)
    public SubscriberResponse getDetail(Long id){

        Subscriber subscriber = getById(id);
        return subscriberMapper.mapToResponse(subscriber);
    }






//cho client
    @Transactional(readOnly = true)
    public ResponseDTO<List<SubscriberListResponse>> getAllSubscriber(int page, int size, SubscriberFilterRequest request) {
        if(request==null){
            request=new SubscriberFilterRequest();
        }


        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));


        Specification<Subscriber> spec = Specification
                .where(SubscriberSpecification.hasUserId(securityUtils.getCurrentUserId()))
                .and(SubscriberSpecification.keyword(request.getKeyword()))
                .and(SubscriberSpecification.hasStatus(request.getStatus()))
                .and(SubscriberSpecification.hasSkills(request.getSkillIds()))
                .and(SubscriberSpecification.createdAfter(request.getFromDate()))
                .and(SubscriberSpecification.createdBefore(request.getToDate()));

        Page<Subscriber> pagelist = subscriberRepository.findAll(spec, pageable);

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<SubscriberListResponse> list = pagelist.getContent().stream()
                .map(subscriberMapper::toResponseSubscriberList)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);

    }
    @Transactional
    public void updateSubscription(Long id, boolean subscribed) {
        Subscriber subscriber = getById(id);

        if (!subscriber.getUser().getId().equals(securityUtils.getCurrentUserId())) {
            throw new ForbiddenException("You don't have permission!");
        }

        subscriber.setSubscribed(subscribed);
    }




}
//    Với cải tiến này:
//
// ✔ Không load tất cả subscriber vào RAM
//✔ Chạy batch rất lớn (10k – 1M subscriber) vẫn ổn
//✔ Email đúng người đúng kỹ năng
//✔ Mỗi subscriber chỉ nhận TOP 10 job mới nhất
//✔ Tối ưu hiệu năng, giảm thời gian chạy scheduled
//✔ Code sạch & dễ mở rộng