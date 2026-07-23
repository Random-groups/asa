package school.hei.asa.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gen.patrimoine.modele.Argent;
import gen.patrimoine.modele.Devise;
import java.time.Month;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.asa.conf.FacadeIT;
import school.hei.asa.model.FinancialPlan;
import school.hei.asa.service.FinancialPlanService;

class FinancialPlanControllerIT extends FacadeIT {

  @Autowired FinancialPlanController subject;

  @MockBean FinancialPlanService financialPlanService;

  @Test
  void oneMonth_complete_studentContract_mockedService() {
    Map<Month, Argent> defaultMonthlyMap = new EnumMap<>(Month.class);
    for (Month month : Month.values()) {
      defaultMonthlyMap.put(month, new Argent(0, Devise.MGA));
    }

    var fakeFinancialPlan = new FinancialPlan(defaultMonthlyMap, defaultMonthlyMap, Map.of());

    when(financialPlanService.financialPlan(2026)).thenReturn(fakeFinancialPlan);

    var result = subject.financialPlan(2026);

    verify(financialPlanService).financialPlan(2026);
    assertTrue(result.contains("plannedCost"));
    assertTrue(result.contains("executedCost"));
    assertTrue(result.contains("koContracts"));
  }
}
