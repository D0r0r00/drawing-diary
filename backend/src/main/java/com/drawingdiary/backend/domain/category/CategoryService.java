package com.drawingdiary.backend.domain.category;

import com.drawingdiary.backend.domain.category.dto.CategoryCreateRequest;
import com.drawingdiary.backend.domain.category.dto.CategoryDeleteResponse;
import com.drawingdiary.backend.domain.category.dto.CategoryResponse;
import com.drawingdiary.backend.domain.category.dto.CategoryUpdateRequest;
import com.drawingdiary.backend.domain.category.exception.CategoryNotFoundException;
import com.drawingdiary.backend.domain.category.exception.DuplicateCategoryNameException;
import com.drawingdiary.backend.domain.category.exception.NotCategoryOwnerException;
import com.drawingdiary.backend.domain.diary.DiaryRepository;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findMine(Long userId) {
        return categoryRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }

    /**
     * 중복은 조회로 먼저 걸러 409를 주고, 그래도 빠져나간 경우를 UNIQUE(user_id, name)
     * 위반으로 다시 잡는다. 같은 이름을 동시에 두 번 만들면 앞선 조회만으로는 둘 다 통과할
     * 수 있는데, 그때 제약 위반이 500으로 새어나가지 않게 하려는 것이다.
     */
    @Transactional
    public CategoryResponse create(Long userId, CategoryCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (categoryRepository.existsByUserIdAndName(userId, request.name())) {
            throw new DuplicateCategoryNameException(request.name());
        }

        try {
            Category saved = categoryRepository.saveAndFlush(Category.builder()
                    .user(user)
                    .name(request.name())
                    .build());
            return new CategoryResponse(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCategoryNameException(request.name());
        }
    }

    /**
     * 이름이 그대로인 요청은 중복 검사를 건너뛴다 — 자기 자신과 충돌한다고 볼 이유가 없다.
     */
    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryUpdateRequest request) {
        Category category = getOwnedCategoryOrThrow(userId, categoryId);

        if (!category.getName().equals(request.name())
                && categoryRepository.existsByUserIdAndName(userId, request.name())) {
            throw new DuplicateCategoryNameException(request.name());
        }

        category.rename(request.name());
        return new CategoryResponse(category.getId(), category.getName());
    }

    /**
     * 카테고리를 지워도 그 카테고리로 분류된 일기는 남긴다. 분류는 일기에 붙은 꼬리표일
     * 뿐이라, 꼬리표를 떼는 행동이 일기 자체를 지우는 결과가 되어서는 안 된다.
     *
     * diaries.category_id에 ON DELETE SET NULL이 걸려 있지 않으므로, 참조를 먼저 끊지
     * 않으면 FK 위반으로 삭제가 실패한다.
     */
    @Transactional
    public CategoryDeleteResponse delete(Long userId, Long categoryId) {
        Category category = getOwnedCategoryOrThrow(userId, categoryId);

        diaryRepository.clearCategory(categoryId);
        categoryRepository.delete(category);

        return new CategoryDeleteResponse("카테고리가 삭제되었습니다");
    }

    /**
     * 없는 카테고리는 404, 남의 카테고리는 403으로 나뉜다. 후자를 404로 감추면 남의 id를
     * 훑어 존재 여부를 알아낼 수 없다는 장점은 있지만, 요구된 스펙이 403이므로 그대로 따른다.
     */
    private Category getOwnedCategoryOrThrow(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!category.getUser().getId().equals(userId)) {
            throw new NotCategoryOwnerException(categoryId);
        }

        return category;
    }
}
