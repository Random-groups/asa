package school.hei.asa.endpoint.rest.controller;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.io.File;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import school.hei.asa.endpoint.rest.controller.mapper.ThInvoiceFormMapper;
import school.hei.asa.endpoint.rest.model.th.ThInvoiceForm;
import school.hei.asa.endpoint.rest.security.WorkerFromAuthentication;
import school.hei.asa.endpoint.rest.service.InvoicePDFGenerator;
import school.hei.asa.service.InvoiceService;

@Controller
@AllArgsConstructor
public class DownloadController {
  private final WorkerFromAuthentication workerFromAuthentication;
  private final WorkerToModelAdder workerToModelAdder;
  private final InvoicePDFGenerator invoicePDFGenerator;
  private final InvoiceService invoiceService;
  private final ThInvoiceFormMapper thInvoiceFormMapper;

  @GetMapping("/download-contract")
  public String redirectToContractsPage() {
    return "redirect:/contracts";
  }

  @GetMapping("/download-invoice")
  public ResponseEntity<Resource> downloadInvoice(
      Model model, Authentication authentication, @RequestParam String yearMonth) {
    var workerCodeOrAuth = workerFromAuthentication.apply(authentication).get().code();
    var pattern = DateTimeFormatter.ofPattern("yyyy-MM");
    var date = YearMonth.parse(yearMonth, pattern);

    model.addAttribute("year", date.getYear());
    var worker = workerToModelAdder.apply(workerCodeOrAuth, model);

    var invoiceForm =
        new ThInvoiceForm(
            null, yearMonth, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null);
    var invoiceData =
        invoiceService.extractInvoiceData(worker, thInvoiceFormMapper.toDomain(invoiceForm));
    var thInvoiceData = thInvoiceFormMapper.toTh(invoiceData);
    File pdfFile = invoicePDFGenerator.apply(worker, thInvoiceData, "invoice");
    FileSystemResource resource = new FileSystemResource(pdfFile);

    var fileName = invoiceService.generateInvoiceFileName(worker);
    return ResponseEntity.ok()
        .contentType(APPLICATION_PDF)
        .header(CONTENT_DISPOSITION, "attachment; filename=" + fileName)
        .body(resource);
  }
}
