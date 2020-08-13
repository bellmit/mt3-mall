package com.hw.aggregate.filter.model;

import com.hw.aggregate.filter.BizFilterRepository;
import com.hw.aggregate.filter.command.CreateBizFilterCommand;
import com.hw.aggregate.filter.command.UpdateBizFilterCommand;
import com.hw.shared.Auditable;
import com.hw.shared.LinkedHashSetConverter;
import com.hw.shared.rest.IdBasedEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

@Data
@Entity
@Table(name = "biz_filter")
@NoArgsConstructor
public class BizFilter extends Auditable implements IdBasedEntity {
    @Id
    private Long id;
    public transient static final String ID_LITERAL = "id";
    @Convert(converter = LinkedHashSetConverter.class)
    private Set<String> linkedCatalog;
    public transient static final String LINKED_CATALOG_LITERAL = "linkedCatalog";
    @Column(length = 10000)
    private ArrayList<BizFilterItem> filterItems;

    @Data
    public static class BizFilterItem implements Serializable {
        private static final long serialVersionUID = 1;
        private Long id;
        private String name;
        private Set<String> selectValues;
    }

    public static BizFilter create(Long id, CreateBizFilterCommand command) {
        return new BizFilter(id, command);
    }

    public void replace(UpdateBizFilterCommand command) {
        this.linkedCatalog = command.getCatalogs();
        this.filterItems = new ArrayList<>();
        command.getFilters().forEach(e -> {
            BizFilterItem bizFilterItem = new BizFilterItem();
            bizFilterItem.setId(e.getId());
            bizFilterItem.setName(e.getName());
            bizFilterItem.setSelectValues(e.getValues());
            this.filterItems.add(bizFilterItem);
        });
    }

    private BizFilter(Long id, CreateBizFilterCommand command) {
        this.id = id;
        this.linkedCatalog = command.getCatalogs();
        this.filterItems = new ArrayList<>();
        command.getFilters().forEach(e -> {
            BizFilterItem bizFilterItem = new BizFilterItem();
            bizFilterItem.setId(e.getId());
            bizFilterItem.setName(e.getName());
            bizFilterItem.setSelectValues(e.getValues());
            this.filterItems.add(bizFilterItem);
        });
    }
}
