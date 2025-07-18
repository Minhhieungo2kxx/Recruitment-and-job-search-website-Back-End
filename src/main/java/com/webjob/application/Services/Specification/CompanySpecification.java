package com.webjob.application.Services.Specification;

import com.webjob.application.Models.Company;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public class CompanySpecification {
    public static Specification<Company> hasName(String name){
        return (Root<Company> root, CriteriaQuery<?> query, CriteriaBuilder builder)->{
            if (name == null || name.isEmpty()){
                return builder.conjunction();
            }
          return builder.like(root.get("name"),"%"+name+"%");
        };
    }

    public static Specification<Company> hasDescription(String description) {
        return (Root<Company> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            if (description == null || description.isEmpty())  return builder.conjunction();
            return builder.like(root.get("description"), "%" + description + "%");
        };
    }

    public static Specification<Company> hasAddress(String address) {
        return (Root<Company> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            if (address == null || address.isEmpty())  return builder.conjunction();
            return builder.like(root.get("address"), "%" + address + "%");
        };
    }



    public static Specification<Company> hasCreatedAt(Instant createdAt) {
        return (Root<Company> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            if (createdAt == null)  return builder.conjunction();
            return builder.equal(root.get("createdAt"), createdAt);
        };
    }

}
