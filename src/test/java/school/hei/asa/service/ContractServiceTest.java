package school.hei.asa.service;

import static java.time.ZoneId.systemDefault;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import school.hei.asa.CareProductCodeSupplier;
import school.hei.asa.model.DailyExecution;
import school.hei.asa.model.Mission;
import school.hei.asa.model.MissionExecution;
import school.hei.asa.model.Product;
import school.hei.asa.model.Worker;
import school.hei.asa.model.contract.Contract;
import school.hei.asa.model.contract.ContractLevel;
import school.hei.asa.model.contract.ContractType;
import school.hei.asa.repository.ContractRepository;
import school.hei.asa.repository.DailyExecutionRepository;
import school.hei.asa.repository.WorkerRepository;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

  @Mock WorkerRepository workerRepository;
  @Mock ContractRepository contractRepository;
  @Mock DailyExecutionRepository dailyExecutionRepository;
  @Mock MissionService missionService;
  @Mock CareProductCodeSupplier careProductCodeSupplier;

  ContractService contractService;

  Worker worker;
  ContractLevel level;
  Contract contract5Days;
  Instant entranceInstant;
  Product product;
  Mission mission;
  LocalDate today;

  @BeforeEach
  void setUp() {
    contractService =
        new ContractService(
            workerRepository,
            contractRepository,
            dailyExecutionRepository,
            missionService,
            careProductCodeSupplier);

    worker = new Worker("code", "name", "email", "fullname", "address", "city", "nif", "stat");
    level = new ContractLevel("level", ContractType.partnerContractor, null, 50000.0);
    entranceInstant = LocalDate.of(2026, 1, 1).atStartOfDay(systemDefault()).toInstant();
    today = LocalDate.of(2026, 6, 17);
    product = new Product("pcode", "pname", "pdescription");
    mission = new Mission("mission-code", "title", "description", 10, product);
  }

  private Contract contractWithDuration(long days) {
    return new Contract(
        worker, "job", level, entranceInstant, null, Duration.ofDays(days), "company", "bucket");
  }

  private DailyExecution workDay(LocalDate date) {
    return new DailyExecution(
        worker,
        date,
        List.of(new MissionExecution(mission, worker, date, 1.0, "comment", Instant.now())));
  }

  @Test
  void no_active_contract_throws() {
    when(contractRepository.findActiveContractByWorkerAtDate(worker.code(), today))
        .thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> contractService.assertRemainingDays(worker.code(), today));
  }

  @Test
  void active_contract_with_remaining_days_does_not_throw() {
    contract5Days = contractWithDuration(5);
    when(contractRepository.findActiveContractByWorkerAtDate(worker.code(), today))
        .thenReturn(Optional.of(contract5Days));
    when(dailyExecutionRepository.findByWorkerCodeAndDateBetween(
            eq(worker.code()), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(
            List.of(
                workDay(today.minusDays(2)),
                workDay(today.minusDays(1))));
    when(careProductCodeSupplier.get()).thenReturn("care-code");

    assertDoesNotThrow(() -> contractService.assertRemainingDays(worker.code(), today));
  }

  @Test
  void active_contract_with_exact_days_used_throws() {
    contract5Days = contractWithDuration(2);
    when(contractRepository.findActiveContractByWorkerAtDate(worker.code(), today))
        .thenReturn(Optional.of(contract5Days));
    when(dailyExecutionRepository.findByWorkerCodeAndDateBetween(
            eq(worker.code()), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(
            List.of(
                workDay(today.minusDays(2)),
                workDay(today.minusDays(1))));
    when(careProductCodeSupplier.get()).thenReturn("care-code");

    assertThrows(
        IllegalArgumentException.class,
        () -> contractService.assertRemainingDays(worker.code(), today));
  }

  @Test
  void active_contract_with_exceeded_days_throws() {
    contract5Days = contractWithDuration(1);
    when(contractRepository.findActiveContractByWorkerAtDate(worker.code(), today))
        .thenReturn(Optional.of(contract5Days));
    when(dailyExecutionRepository.findByWorkerCodeAndDateBetween(
            eq(worker.code()), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(
            List.of(
                workDay(today.minusDays(2)),
                workDay(today.minusDays(1))));
    when(careProductCodeSupplier.get()).thenReturn("care-code");

    assertThrows(
        IllegalArgumentException.class,
        () -> contractService.assertRemainingDays(worker.code(), today));
  }

  @Test
  void active_contract_with_fractional_remaining_days_does_not_throw() {
    contract5Days = contractWithDuration(5);
    when(contractRepository.findActiveContractByWorkerAtDate(worker.code(), today))
        .thenReturn(Optional.of(contract5Days));
    when(dailyExecutionRepository.findByWorkerCodeAndDateBetween(
            eq(worker.code()), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(
            List.of(
                new DailyExecution(
                    worker,
                    today.minusDays(2),
                    List.of(
                        new MissionExecution(
                            mission, worker, today.minusDays(2), 0.5, "comment", Instant.now()),
                        new MissionExecution(
                            mission, worker, today.minusDays(2), 0.5, "comment2", Instant.now()))),
                workDay(today.minusDays(1))));
    when(careProductCodeSupplier.get()).thenReturn("care-code");

    assertDoesNotThrow(() -> contractService.assertRemainingDays(worker.code(), today));
  }
}
