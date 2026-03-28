package com.lendiq.apigateway.repository.spec;

import com.lendiq.dto.request.ApplicationFilterRequest;
import com.lendiq.apigateway.entity.Application;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationSpec {

    public static Specification<Application> withFilters(ApplicationFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), f.getStatus()));
            if (f.getFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.getFrom()));
            if (f.getTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.getTo()));
            if (f.getChannel() != null)
                predicates.add(cb.equal(root.get("sourceChannel"), f.getChannel()));
            if (f.getApplicantId() != null)
                predicates.add(cb.equal(root.get("applicantId"), f.getApplicantId()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}