package com.webjob.application.service;

import com.webjob.application.dto.Request.SkillRequest;
import com.webjob.application.dto.Request.SkillSearchRequest;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SkillOptionResponse;
import com.webjob.application.dto.Response.SkillResponse;
import com.webjob.application.enums.SkillStatus;
import com.webjob.application.mapper.SkillMapper;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.SkillAlias;
import com.webjob.application.repository.*;
import com.webjob.application.utils.common.UtilFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.StatusResultMatchersExtensionsKt.isEqualTo;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService Unit Tests")
public class SkillServiceTest {
    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SkillMapper skillMapper;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @Mock
    private SubscriberSkillRepository subscriberSkillRepository;

    @Mock
    private JobCategorySkillRepository jobCategorySkillRepository;

    @Mock
    private SkillAliasRepository skillAliasRepository;

    @InjectMocks
    private SkillService skillService;

    private Skill createSkill(Long id, String name) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName(name);
        skill.setAliases(new ArrayList<>());
        return skill;
    }

    private SkillRequest createRequest(String name) {
        SkillRequest request = mock(SkillRequest.class);
        lenient().when(request.getName()).thenReturn(name);
        lenient().when(request.getAliases()).thenReturn(null);
        return request;
    }

    // -------------------------------------------------------
    // checkNameskill
    // -------------------------------------------------------

    @Test
    void checkNameskill_whenNameDoesNotExist_returnsFalse() {
        when(skillRepository.existsByName("Java")).thenReturn(false);

        boolean result = skillService.checkNameskill("Java");

        assertThat(result).isFalse();
        verify(skillRepository).existsByName("Java");
    }

    @Test
    void checkNameskill_whenNameAlreadyExists_throwsException() {
        when(skillRepository.existsByName("Java")).thenReturn(true);

        assertThatThrownBy(() -> skillService.checkNameskill("Java"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Skill name Java da ton tai");
    }

    // -------------------------------------------------------
    // checkById / getbyID
    // -------------------------------------------------------
    @Test
    void checkById_whenSkillExists_returnsTrue() {
        when(skillRepository.existsById(1L)).thenReturn(true);
        assertThat(skillService.checkById(1L)).isTrue();
    }

    @Test
    void checkById_whenSkillDoesNotExist_throwsException() {
        when(skillRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> skillService.checkById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tồn tại Skill với ID: 99");
    }

    @Test
    void getbyID_returnsRepositoryResult() {
        Skill skill = createSkill(1L, "Java");
        when(skillRepository.findById(1L))
                .thenReturn(Optional.of(skill));

        assertThat(skillService.getbyID(1L))
                .containsSame(skill);

        when(skillRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThat(skillService.getbyID(2L)).isEmpty();
    }

    // -------------------------------------------------------
    // getAllPage
    // -------------------------------------------------------

    @Test
    void getAllPage_usesRequestedPageAndSortsByIdAscending() {
        Pageable expectedPageable = PageRequest.of(
                1, 10, Sort.by(Sort.Direction.ASC, "id"));

        Page<Skill> expectedPage = new PageImpl<>(
                List.of(createSkill(11L, "Java")));

        when(skillRepository.findAll(expectedPageable))
                .thenReturn(expectedPage);

        Page<Skill> result = skillService.getAllPage(1, 10);

        assertThat(result).isSameAs(expectedPage);
        verify(skillRepository).findAll(expectedPageable);
    }

    // -------------------------------------------------------
    // getAllPageList
    // -------------------------------------------------------

    @Test
    void getAllPageList_whenSkillsExist_mapsSkillsAndAliases() {
        Skill skill = createSkill(1L, "Java");
        SkillAlias alias = SkillAlias.builder()
                .skill(skill)
                .alias("Java SE")
                .normalizedAlias("java se")
                .build();

        Page<Skill> page = new PageImpl<>(
                List.of(skill),
                PageRequest.of(0, 10, Sort.by(
                        Sort.Direction.ASC, "id")),
                1
        );

        SkillResponse response = mock(SkillResponse.class);

        when(skillRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        when(skillAliasRepository.findAllBySkillIds(List.of(1L)))
                .thenReturn(List.of(alias));

        when(skillMapper.toResponsePage(skill, List.of(alias)))
                .thenReturn(response);

        ResponseDTO<List<SkillResponse>> result = skillService.getAllPageList(1, 10);

        assertThat(result.getResult()).containsExactly(response);
        verify(skillAliasRepository).findAllBySkillIds(List.of(1L));
        verify(skillMapper).toResponsePage(skill, List.of(alias));
    }

    @Test
    void getAllPageList_whenPageIsEmpty_doesNotQueryAliases() {
        Page<Skill> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0
        );

        when(skillRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        ResponseDTO<List<SkillResponse>> result =
                skillService.getAllPageList(0, 0);

        assertThat(result.getResult()).isEmpty();

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(skillRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);

        verify(skillAliasRepository, never())
                .findAllBySkillIds(anyList());
        verifyNoInteractions(skillMapper);
    }

    @Test
    void getAllPageList_capsPageSizeAt50() {
        when(skillRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        Collections.emptyList(),
                        PageRequest.of(0, 50),
                        0
                ));

        skillService.getAllPageList(1, 100);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(skillRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    // -------------------------------------------------------
    // deleteSkill
    // -------------------------------------------------------

    @Test
    void deleteSkill_whenSkillExists_deletesAllRelationsThenSkill() {
        when(skillRepository.existsById(1L)).thenReturn(true);

        skillService.deleteSkill(1L);

        InOrder inOrder = inOrder(
                jobSkillRepository,
                subscriberSkillRepository,
                jobCategorySkillRepository,
                skillRepository
        );

        inOrder.verify(jobSkillRepository).deleteBySkillId(1L);
        inOrder.verify(subscriberSkillRepository).deleteBySkillId(1L);
        inOrder.verify(jobCategorySkillRepository).deleteBySkillId(1L);
        inOrder.verify(skillRepository).deleteById(1L);
    }

    @Test
    void deleteSkill_whenSkillDoesNotExist_throwsException() {
        when(skillRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> skillService.deleteSkill(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Skill không tồn tại với id: 99");

        verify(skillRepository, never()).deleteById(anyLong());
        verifyNoInteractions(
                jobSkillRepository,
                subscriberSkillRepository,
                jobCategorySkillRepository
        );
    }

    // -------------------------------------------------------
    // createSkill
    // -------------------------------------------------------
    @Test
    void createSkill_withoutAliases_savesAndReturnsResponse() {
        SkillRequest request = createRequest("Java");

        Skill skill = createSkill(null, "Java");
        Skill saved = createSkill(1L, "Java");
        SkillResponse expected = mock(SkillResponse.class);

        when(skillRepository.existsByName("Java")).thenReturn(false);
        when(modelMapper.map(request, Skill.class)).thenReturn(skill);
        when(skillRepository.save(skill)).thenReturn(saved);

        when(skillMapper.toResponse(saved)).thenReturn(expected);

        SkillResponse result = skillService.createSkill(request);

        assertThat(result).isSameAs(expected);
        assertThat(skill.getAliases()).isEmpty();

        verify(skillRepository).save(skill);
        verify(skillMapper).toResponse(saved);
    }

    @Test
    void createSkill_whenNameAlreadyExists_throwsExceptionWithoutSaving() {
        SkillRequest request = createRequest("Java");
        when(skillRepository.existsByName("Java")).thenReturn(true);

        assertThatThrownBy(() -> skillService.createSkill(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Skill name Java da ton tai");

        verify(skillRepository, never()).save(any(Skill.class));
        verifyNoInteractions(modelMapper, skillMapper);
    }

    @Test
    void createSkill_withAliases_mapsAliasFieldsAndNormalizedValue() {
        SkillRequest request = createRequest("Java");

        SkillRequest.SkillAliasRequest aliasRequest =
                mock(SkillRequest.SkillAliasRequest.class);

        when(aliasRequest.getAlias()).thenReturn("Java SE");
        when(aliasRequest.getAliasType()).thenReturn(null);
        when(aliasRequest.getStatus()).thenReturn(null);
        when(request.getAliases()).thenReturn(List.of(aliasRequest));

        Skill skill = createSkill(null, "Java");
        Skill saved = createSkill(1L, "Java");

        when(skillRepository.existsByName("Java")).thenReturn(false);
        when(modelMapper.map(request, Skill.class)).thenReturn(skill);
        when(skillRepository.save(skill)).thenReturn(saved);
        when(skillMapper.toResponse(saved)).thenReturn(
                mock(SkillResponse.class));

        skillService.createSkill(request);

        assertThat(skill.getAliases()).hasSize(1);

        SkillAlias createdAlias = skill.getAliases().get(0);
        assertThat(createdAlias.getSkill()).isSameAs(skill);
        assertThat(createdAlias.getAlias()).isEqualTo("Java SE");
        assertThat(createdAlias.getNormalizedAlias())
                .isEqualTo(UtilFormat.normalize("Java SE"));
    }

    // -------------------------------------------------------
    // updateSkill
    // -------------------------------------------------------

    @Test
    void updateSkill_whenIdDoesNotExist_throwsException() {
        SkillRequest request = createRequest("Java");
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.updateSkill(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Skill not found with ID: 99");

        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void updateSkill_whenNameAlreadyBelongsToAnotherSkill_throwsException() {
        Skill skill = createSkill(1L, "Java");
        SkillRequest request = createRequest("Python");

        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Python", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> skillService.updateSkill(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Skill name 'Python' already exists");

        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void updateSkill_whenNameIsBlank_keepsExistingName() {
        Skill skill = createSkill(1L, "Java");
        SkillRequest request = createRequest("  ");

        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java", 1L))
                .thenReturn(false);
        when(skillRepository.save(skill)).thenReturn(skill);
        when(skillMapper.toResponse(skill))
                .thenReturn(mock(SkillResponse.class));

        skillService.updateSkill(1L, request);

        assertThat(skill.getName()).isEqualTo("Java");
        verify(skillRepository).save(skill);
    }

    @Test
    void updateSkill_whenAliasIsBlank_throwsExceptionWithoutSaving() {
        Skill skill = createSkill(1L, "Java");
        SkillRequest request = createRequest("Java");

        SkillRequest.SkillAliasRequest aliasRequest =
                mock(SkillRequest.SkillAliasRequest.class);
        when(aliasRequest.getAlias()).thenReturn("  ");
        when(request.getAliases()).thenReturn(List.of(aliasRequest));

        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java", 1L))
                .thenReturn(false);

        assertThatThrownBy(() -> skillService.updateSkill(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alias must not be blank");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(skillAliasRepository, never()).deleteBySkillId(anyLong());
    }

    @Test
    void updateSkill_whenAliasesHaveDuplicateNormalizedValues_throwsException() {
        Skill skill = createSkill(1L, "Java");
        SkillRequest request = createRequest("Java");

        SkillRequest.SkillAliasRequest first =
                mock(SkillRequest.SkillAliasRequest.class);
        SkillRequest.SkillAliasRequest second =
                mock(SkillRequest.SkillAliasRequest.class);

        when(first.getAlias()).thenReturn("Java SE");
        when(second.getAlias()).thenReturn(" java se ");
        when(request.getAliases()).thenReturn(List.of(first, second));

        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java", 1L))
                .thenReturn(false);

        assertThatThrownBy(() -> skillService.updateSkill(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate alias:  java se ");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(skillAliasRepository, never()).deleteBySkillId(anyLong());
    }

    @Test
    void updateSkill_withValidAliases_replacesOldAliasesAndSaves() {
        Skill skill = createSkill(1L, "Java");

        SkillAlias oldAlias = SkillAlias.builder()
                .skill(skill)
                .alias("Old alias")
                .build();

        skill.getAliases().add(oldAlias);

        SkillRequest request = createRequest("Java");
        SkillRequest.SkillAliasRequest aliasRequest =
                mock(SkillRequest.SkillAliasRequest.class);

        when(aliasRequest.getAlias()).thenReturn("Java SE");
        when(aliasRequest.getAliasType()).thenReturn(null);
        when(aliasRequest.getStatus()).thenReturn(null);
        when(request.getAliases()).thenReturn(List.of(aliasRequest));

        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java", 1L))
                .thenReturn(false);
        when(skillRepository.save(skill)).thenReturn(skill);
        when(skillMapper.toResponse(skill))
                .thenReturn(mock(SkillResponse.class));

        skillService.updateSkill(1L, request);

        InOrder inOrder = inOrder(skillAliasRepository, skillRepository);
        inOrder.verify(skillAliasRepository).deleteBySkillId(1L);
        inOrder.verify(skillAliasRepository).flush();
        inOrder.verify(skillRepository).save(skill);

        assertThat(skill.getAliases()).hasSize(1);
        assertThat(skill.getAliases().get(0).getAlias())
                .isEqualTo("Java SE");
    }

    // -------------------------------------------------------
    // getSkillByID
    // -------------------------------------------------------
    @Test
    void getSkillByID_whenSkillExists_returnsMappedResponse() {
        Skill skill = createSkill(1L, "Java");
        SkillResponse expected = mock(SkillResponse.class);

        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(skillMapper.toResponse(skill)).thenReturn(expected);

        assertThat(skillService.getSkillByID(1L)).isSameAs(expected);
    }

    @Test
    void getSkillByID_whenSkillDoesNotExist_throwsException() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillByID(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Skill not found with ID: 99");

        verifyNoInteractions(skillMapper);
    }

    // -------------------------------------------------------
    // searchSkills
    // -------------------------------------------------------

    @Test
    void searchSkills_delegatesSpecificationAndPageableToRepository() {
        SkillSearchRequest request = mock(SkillSearchRequest.class);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Skill> expected = new PageImpl<>(
                List.of(createSkill(1L, "Java")));

        when(skillRepository.findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                eq(pageable)
        )).thenReturn(expected);

        Page<Skill> result = skillService.searchSkills(request, pageable);

        assertThat(result).isSameAs(expected);

        verify(skillRepository).findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                eq(pageable)
        );
    }

    // -------------------------------------------------------
    // searchSkill
    // -------------------------------------------------------

    @Test
    void searchSkill_whenRequestIsNull_usesDefaultRequest() {
        when(skillRepository.findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0
        ));

        ResponseDTO<List<SkillResponse>> result =
                skillService.searchSkill(null, 0, 10);

        assertThat(result.getResult()).isEmpty();

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(skillRepository).findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                captor.capture()
        );

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        verify(skillAliasRepository, never())
                .findAllBySkillIds(anyList());
    }

    @Test
    void searchSkill_whenResultsExist_mapsAliasesAndResponses() {
        SkillSearchRequest request = mock(SkillSearchRequest.class);
        Skill skill = createSkill(1L, "Java");
        SkillAlias alias = SkillAlias.builder()
                .skill(skill)
                .alias("Java SE")
                .build();
        SkillResponse response = mock(SkillResponse.class);

        when(skillRepository.findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(skill),
                PageRequest.of(0, 10),
                1
        ));

        when(skillAliasRepository.findAllBySkillIds(List.of(1L)))
                .thenReturn(List.of(alias));
        when(skillMapper.toResponsePage(skill, List.of(alias)))
                .thenReturn(response);

        ResponseDTO<List<SkillResponse>> result =
                skillService.searchSkill(request, 1, 10);

        assertThat(result.getResult()).containsExactly(response);
        verify(skillMapper).toResponsePage(skill, List.of(alias));
    }

    // -------------------------------------------------------
    // searchSkillforSubscriber
    // -------------------------------------------------------

    @Test
    void searchSkillforSubscriber_mapsMatchingSkillsToOptions() {
        Skill skill = createSkill(1L, "Java");
        SkillOptionResponse option = mock(SkillOptionResponse.class);

        when(skillRepository.findAll(
                ArgumentMatchers.<Specification<Skill>>any()
        )).thenReturn(List.of(skill));
        when(modelMapper.map(skill, SkillOptionResponse.class))
                .thenReturn(option);

        List<SkillOptionResponse> result =
                skillService.searchSkillforSubscriber("Java");

        assertThat(result).containsExactly(option);
        verify(modelMapper).map(skill, SkillOptionResponse.class);
    }

    @Test
    void searchSkillforSubscriber_whenNoSkillsFound_returnsEmptyList() {
        when(skillRepository.findAll(
                ArgumentMatchers.<Specification<Skill>>any()
        )).thenReturn(Collections.emptyList());

        List<SkillOptionResponse> result =
                skillService.searchSkillforSubscriber("unknown");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }


    @Test
    void updateSkill_whenAliasesAreNull_preservesExistingAliases() {
        Skill skill = createSkill(1L, "Java");
        SkillAlias oldAlias = SkillAlias.builder()
                .skill(skill)
                .alias("Java SE")
                .build();
        skill.getAliases().add(oldAlias);

        SkillRequest request = createRequest("Java");

        when(skillRepository.findById(1L))
                .thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java", 1L))
                .thenReturn(false);
        when(skillRepository.save(skill)).thenReturn(skill);
        when(skillMapper.toResponse(skill))
                .thenReturn(mock(SkillResponse.class));

        skillService.updateSkill(1L, request);

        assertThat(skill.getAliases()).containsExactly(oldAlias);

        verify(skillAliasRepository, never()).deleteBySkillId(1L);
        verify(skillRepository).save(skill);
    }


    @Test
    void updateSkill_whenAliasesAreEmpty_removesAllOldAliases() {
        Skill skill = createSkill(1L, "Java");
        skill.getAliases().add(
                SkillAlias.builder()
                        .skill(skill)
                        .alias("Java SE")
                        .build()
        );

        SkillRequest request = createRequest("Java");
        when(request.getAliases()).thenReturn(Collections.emptyList());

        when(skillRepository.findById(1L))
                .thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java", 1L))
                .thenReturn(false);
        when(skillRepository.save(skill)).thenReturn(skill);
        when(skillMapper.toResponse(skill))
                .thenReturn(mock(SkillResponse.class));

        skillService.updateSkill(1L, request);

        assertThat(skill.getAliases()).isEmpty();

        InOrder inOrder = inOrder(skillAliasRepository, skillRepository);
        inOrder.verify(skillAliasRepository).deleteBySkillId(1L);
        inOrder.verify(skillAliasRepository).flush();
        inOrder.verify(skillRepository).save(skill);
    }

    @Test
    void updateSkill_whenDescriptionAndStatusAreProvided_updatesFields() {
        Skill skill = createSkill(1L, "Java");

        SkillRequest request = createRequest("Java");
        when(request.getDescription()).thenReturn("Programming language");
        when(request.getStatus()).thenReturn(SkillStatus.ACTIVE);

        when(skillRepository.findById(1L))
                .thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java", 1L))
                .thenReturn(false);
        when(skillRepository.save(skill)).thenReturn(skill);
        when(skillMapper.toResponse(skill))
                .thenReturn(mock(SkillResponse.class));

        skillService.updateSkill(1L, request);

        assertThat(skill.getDescription())
                .isEqualTo("Programming language");
        assertThat(skill.getStatus()).isEqualTo(SkillStatus.ACTIVE);

        verify(skillRepository).save(skill);
    }


    @Test
    void searchSkill_whenPageAndSizeAreOutOfRange_clampsPagination() {
        when(skillRepository.findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 50),
                0
        ));

        skillService.searchSkill(null, 0, 100);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(skillRepository).findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                captor.capture()
        );

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }


    @Test
    void searchSkill_whenMultipleSkillsExist_groupsAliasesBySkillId() {
        Skill java = createSkill(1L, "Java");
        Skill python = createSkill(2L, "Python");

        SkillAlias javaAlias = SkillAlias.builder()
                .skill(java)
                .alias("Java SE")
                .build();

        SkillAlias pythonAlias = SkillAlias.builder()
                .skill(python)
                .alias("Python 3")
                .build();

        SkillResponse javaResponse = mock(SkillResponse.class);
        SkillResponse pythonResponse = mock(SkillResponse.class);

        when(skillRepository.findAll(
                ArgumentMatchers.<Specification<Skill>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(java, python),
                PageRequest.of(0, 10),
                2
        ));

        when(skillAliasRepository.findAllBySkillIds(List.of(1L, 2L)))
                .thenReturn(List.of(javaAlias, pythonAlias));

        when(skillMapper.toResponsePage(java, List.of(javaAlias)))
                .thenReturn(javaResponse);

        when(skillMapper.toResponsePage(python, List.of(pythonAlias)))
                .thenReturn(pythonResponse);

        ResponseDTO<List<SkillResponse>> result =
                skillService.searchSkill(null, 1, 10);

        assertThat(result.getResult())
                .containsExactly(javaResponse, pythonResponse);

        verify(skillMapper).toResponsePage(java, List.of(javaAlias));
        verify(skillMapper).toResponsePage(python, List.of(pythonAlias));
    }


    @Test
    void updateSkill_whenNewNameIsAvailable_updatesName() {
        Skill skill = createSkill(1L, "Java");
        SkillRequest request = createRequest("Java Backend");

        when(skillRepository.findById(1L))
                .thenReturn(Optional.of(skill));
        when(skillRepository.existsByNameAndIdNot("Java Backend", 1L))
                .thenReturn(false);
        when(skillRepository.save(skill)).thenReturn(skill);
        when(skillMapper.toResponse(skill))
                .thenReturn(mock(SkillResponse.class));

        skillService.updateSkill(1L, request);

        assertThat(skill.getName()).isEqualTo("Java Backend");
        verify(skillRepository).save(skill);
    }



}
