package com.campusos.exam;

import com.campusos.common.ApiException;
import com.campusos.exam.dto.ExamDtos.*;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarksheetPdfService {

    private final MarksheetJobRepository jobRepository;
    private final StudentRepository studentRepository;
    private final ExamResultRepository resultRepository;
    private final GradeService gradeService;
    private final String storageDir;

    public MarksheetPdfService(MarksheetJobRepository jobRepository,
                               StudentRepository studentRepository,
                               ExamResultRepository resultRepository,
                               GradeService gradeService,
                               @Value("${app.marksheet.dir:./marksheets}") String storageDir) {
        this.jobRepository = jobRepository;
        this.studentRepository = studentRepository;
        this.resultRepository = resultRepository;
        this.gradeService = gradeService;
        this.storageDir = storageDir;
    }

    /** Called by ExamService after the job row is created; renders on a virtual thread. */
    @Async
    public void renderAsync(String jobId, Long studentId) {
        MarksheetJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;
        try {
            var results = resultRepository.findStudentResults(studentId);
            List<ResultRow> rows = ResultsMapper.toRows(results);
            double cgpa = gradeService.computeGpa(results, null);

            Path dir = Paths.get(storageDir);
            Files.createDirectories(dir);
            Path pdfPath = dir.resolve("marksheet-" + jobId + ".pdf");

            renderPdf(pdfPath, studentRepository.findById(studentId).orElseThrow(), rows, cgpa);
            job.setStatus(MarksheetJob.Status.COMPLETED);
            job.setFilePath(pdfPath.toString());
        } catch (Exception ex) {
            log.error("Marksheet render failed for job {}", jobId, ex);
            job.setStatus(MarksheetJob.Status.FAILED);
            job.setError(ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(499, ex.getMessage().length())) : "render error");
        }
        jobRepository.save(job);
    }

    void renderPdf(Path path, Student student, List<ResultRow> rows, double cgpa) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 48, 36);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
            document.open();

            Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font th = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font td = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("CampusOS Institute of Technology", h1));
            document.add(new Paragraph("Official Academic Marksheet", h2));
            document.add(new Paragraph(" "));

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.addCell(new Paragraph("Student: " + student.getName(), td));
            info.addCell(new Paragraph("Roll No: " + student.getRollNumber(), td));
            info.addCell(new Paragraph("Program: B.Tech", td));
            info.addCell(new Paragraph("Department: " +
                    (student.getDepartment() != null ? student.getDepartment().getName() : "-"), td));
            document.add(info);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            for (String col : new String[]{"Semester", "Course", "Type", "Marks", "Max", "Grade", "Points"}) {
                table.addCell(new Paragraph(col, th));
            }
            for (ResultRow r : rows) {
                table.addCell(new Paragraph(r.semesterName(), td));
                table.addCell(new Paragraph(r.courseCode() + " - " + r.courseTitle(), td));
                table.addCell(new Paragraph(r.examType(), td));
                table.addCell(new Paragraph(String.valueOf(r.marks()), td));
                table.addCell(new Paragraph(String.valueOf(r.maxMarks()), td));
                table.addCell(new Paragraph(r.grade() == null ? "-" : r.grade(), td));
                table.addCell(new Paragraph(r.gradePoints() == null ? "-" : r.gradePoints().toPlainString(), td));
            }
            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("CGPA: " + cgpa, th));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("This is a system-generated document.", h2));
        } finally {
            document.close();
        }
    }

    Path filePath(String jobId) {
        MarksheetJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("Marksheet job not found"));
        if (job.getStatus() != MarksheetJob.Status.COMPLETED || job.getFilePath() == null) {
            throw ApiException.conflict("Marksheet not ready yet (status: " + job.getStatus() + ")");
        }
        return Paths.get(job.getFilePath());
    }

    static final class ResultsMapper {
        static List<ResultRow> toRows(List<ExamResultRepository.StudentResultRow> results) {
            return results.stream()
                    .map(r -> new ResultRow(r.getCourseId(), r.getCourseCode(), r.getCourseTitle(),
                            r.getCredits(), r.getSemesterId(), r.getSemesterName(), r.getExamType(),
                            r.getMarksObtained(), r.getMaxMarks(), r.getGrade(), r.getGradePoints()))
                    .toList();
        }
    }
}
