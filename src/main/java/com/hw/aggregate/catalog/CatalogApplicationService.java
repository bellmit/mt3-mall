package com.hw.aggregate.catalog;

import com.hw.aggregate.catalog.command.CreateCatalogCommand;
import com.hw.aggregate.catalog.command.UpdateCatalogCommand;
import com.hw.aggregate.catalog.model.Catalog;
import com.hw.aggregate.catalog.representation.CatalogRepresentation;
import com.hw.aggregate.catalog.representation.CatalogSummaryAdminRepresentation;
import com.hw.aggregate.catalog.representation.CatalogSummaryCustomerRepresentation;
import com.hw.shared.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CatalogApplicationService {

    @Autowired
    private CatalogRepo categoryRepo;

    @Autowired
    private IdGenerator idGenerator;

    public CatalogSummaryCustomerRepresentation getAllForCustomer() {
        return new CatalogSummaryCustomerRepresentation(categoryRepo.findAll());
    }

    public CatalogSummaryAdminRepresentation getAllForAdmin() {
        return new CatalogSummaryAdminRepresentation(categoryRepo.findAll());
    }

    public CatalogRepresentation create(CreateCatalogCommand command) {
        return new CatalogRepresentation(Catalog.create(idGenerator.getId(), command, categoryRepo));
    }

    public void update(Long id, UpdateCatalogCommand command) {
        Catalog.update(id, command, categoryRepo);
    }

    public void delete(Long id) {
        Catalog.delete(id, categoryRepo);
    }

}
