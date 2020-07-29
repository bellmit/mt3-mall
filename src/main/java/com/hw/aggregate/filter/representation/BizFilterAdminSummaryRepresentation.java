package com.hw.aggregate.filter.representation;

import com.hw.aggregate.filter.model.BizFilter;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class BizFilterAdminSummaryRepresentation {
    private List<BizFilterItemCardRepresentation> data;
    private Integer totalPageCount;
    private Long totalItemCount;

    public BizFilterAdminSummaryRepresentation(List<BizFilter> all, Integer totalPageCount, Long totalItemCount) {
        this.data = all.stream().map(BizFilterItemCardRepresentation::new).collect(Collectors.toList());
        this.totalPageCount = totalPageCount;
        this.totalItemCount = totalItemCount;
    }

    @Data
    private static class BizFilterItemCardRepresentation {
        private Long id;
        private Set<String> catalogs;

        public BizFilterItemCardRepresentation(BizFilter e) {
            id = e.getId();
            catalogs = e.getLinkedCatalog();
        }
    }
}
