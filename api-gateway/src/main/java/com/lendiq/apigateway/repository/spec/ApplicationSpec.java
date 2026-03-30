package com.lendiq.apigateway.repository.spec;

import com.lendiq.apigateway.dto.request.ApplicationFilterRequest;
import com.lendiq.apigateway.entity.Application;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationSpec {

    public static Specification<Application> withFilters(ApplicationFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.status() != null)
                predicates.add(cb.equal(root.get("status"), f.status()));
            if (f.from() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
            if (f.to() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.to()));
            if (f.channel() != null)
                predicates.add(cb.equal(root.get("sourceChannel"), f.channel()));
            if (f.applicantId() != null)
                predicates.add(cb.equal(root.get("applicant").get("id"), f.applicantId()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}