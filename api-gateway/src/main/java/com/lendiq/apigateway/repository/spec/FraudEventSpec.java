package com.lendiq.apigateway.repository.spec;

import com.lendiq.apigateway.entity.FraudEvent;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class FraudEventSpec {

    public static Specification<FraudEvent> withFilters(String eventType, Boolean resolved) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventType != null)
                predicates.add(cb.equal(root.get("eventType"), eventType));
            if (resolved != null)
                predicates.add(cb.equal(root.get("resolved"), resolved));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
