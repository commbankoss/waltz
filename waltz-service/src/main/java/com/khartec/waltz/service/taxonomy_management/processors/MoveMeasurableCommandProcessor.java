package com.khartec.waltz.service.taxonomy_management.processors;

import com.khartec.waltz.common.DateTimeUtilities;
import com.khartec.waltz.common.SetUtilities;
import com.khartec.waltz.model.EntityKind;
import com.khartec.waltz.model.EntityReference;
import com.khartec.waltz.model.measurable.Measurable;
import com.khartec.waltz.model.taxonomy_management.*;
import com.khartec.waltz.service.measurable.MeasurableService;
import com.khartec.waltz.service.taxonomy_management.TaxonomyCommandProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.service.taxonomy_management.TaxonomyManagementUtilities.*;

@Service
public class MoveMeasurableCommandProcessor implements TaxonomyCommandProcessor {

    private final MeasurableService measurableService;


    @Autowired
    public MoveMeasurableCommandProcessor(MeasurableService measurableService) {
        checkNotNull(measurableService, "measurableService cannot be null");
        this.measurableService = measurableService;
    }


    @Override
    public Set<TaxonomyChangeType> supportedTypes() {
        return SetUtilities.asSet(TaxonomyChangeType.MOVE);
    }


    @Override
    public EntityKind domain() {
        return EntityKind.MEASURABLE_CATEGORY;
    }


    public TaxonomyChangePreview preview(TaxonomyChangeCommand cmd) {
        Measurable primaryRef = validate(cmd);
        // can't think of any impacts that could be meaningfully calculated....
        return ImmutableTaxonomyChangePreview
                .builder()
                .command(ImmutableTaxonomyChangeCommand
                        .copyOf(cmd)
                        .withPrimaryReference(primaryRef.entityReference()))
                .build();
    }


    public TaxonomyChangeCommand apply(TaxonomyChangeCommand cmd, String userId) {
        Measurable measurableToMove = validate(cmd);
        measurableService.move(
                measurableToMove.id().get(),
                getDestination(cmd),
                userId);

        return ImmutableTaxonomyChangeCommand
                .copyOf(cmd)
                .withLastUpdatedAt(DateTimeUtilities.nowUtc())
                .withLastUpdatedBy(userId)
                .withStatus(TaxonomyChangeLifecycleStatus.EXECUTED);
    }



    private Measurable validate(TaxonomyChangeCommand cmd) {
        doBasicValidation(cmd);
        long categoryId = cmd.changeDomain().id();
        Measurable m = validateMeasurableInCategory(measurableService, cmd.primaryReference().id(), categoryId);
        Long destinationId = getDestination(cmd);
        if (destinationId != null) {
            validateMeasurableInCategory(measurableService, destinationId, categoryId);
        }
        return m;
    }


    private Long getDestination(TaxonomyChangeCommand cmd) {
        return cmd.paramAsLong("destinationId", null);
    }


    private String getDestinationName(TaxonomyChangeCommand cmd) {
        return cmd.param("destinationName");
    }

}
