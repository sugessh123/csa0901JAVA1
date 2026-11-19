import java.io.IOException;
import java.time.*;
import java.util.*;

/* ===================== Exception hierarchy ===================== */
class ExamException extends Exception {                       // CHECKED root: every portal rule failure
    ExamException(String m) { super(m); }
    ExamException(String m, Throwable cause) { super(m, cause); }
}
class ExamWindowClosedException extends ExamException {       // CHECKED: student can retry / contact admin
    ExamWindowClosedException(String m) { super(m); }
}
class NotRegisteredException extends ExamException {          // CHECKED: needs a human decision
    NotRegisteredException(String m) { super(m); }
}
class FileTooLargeException extends ExamException {           // CHECKED: student can re-upload a smaller file
    FileTooLargeException(String m) { super(m); }
}
class SubmissionStorageException extends ExamException {      // CHECKED: infrastructure failure, keeps cause
    SubmissionStorageException(String m, Throwable cause) { super(m, cause); }
}
class PortalConfigurationException extends RuntimeException { // UNCHECKED: programmer/config bug, not recoverable
    PortalConfigurationException(String m) { super(m); }
}

/* ===================== Storage layer ===================== */
class StorageLayer {
    private final long diskQuotaKb;
    StorageLayer(long diskQuotaKb) { this.diskQuotaKb = diskQuotaKb; }

    void save(String student, String file, long sizeKb) throws SubmissionStorageException {
        try {
            rawWrite(file, sizeKb);                            // low-level, throws IOException
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + file + " for " + student, e); // chained
        }
    }
    /** Same method but re-throws WITHOUT the cause: used only to show what chaining prevents losing. */
    void saveLossy(String student, String file, long sizeKb) throws SubmissionStorageException {
        try {
            rawWrite(file, sizeKb);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + file + " for " + student, null);
        }
    }
    private void rawWrite(String file, long sizeKb) throws IOException {
        if (sizeKb > diskQuotaKb) throw new IOException("Disk quota exceeded (" + sizeKb + " KB > " + diskQuotaKb + " KB)");
    }
}

/* ===================== Service layer ===================== */
class SubmissionService {
    private final LocalTime opens, closes;
    private final Set<String> registered;
    private final long maxSizeKb;
    private final StorageLayer storage;

    SubmissionService(LocalTime opens, LocalTime closes, Set<String> registered, long maxSizeKb, StorageLayer storage) {
        if (!closes.isAfter(opens)) throw new PortalConfigurationException("Exam window closes before it opens");
        this.opens = opens; this.closes = closes; this.registered = registered;
        this.maxSizeKb = maxSizeKb; this.storage = storage;
    }

    void submit(String student, String course, String file, long sizeKb, LocalTime now) throws ExamException {
        if (now.isBefore(opens) || now.isAfter(closes))
            throw new ExamWindowClosedException("Exam window " + opens + "-" + closes + " is closed at " + now);
        if (!registered.contains(student + ":" + course))
            throw new NotRegisteredException(student + " is not registered for " + course);
        if (sizeKb > maxSizeKb)
            throw new FileTooLargeException(file + " is " + sizeKb + " KB; limit is " + maxSizeKb + " KB");
        storage.save(student, file, sizeKb);                   // SubmissionStorageException propagates upward
    }
}

/* ===================== Controller layer (final handler) ===================== */
class SubmissionController {
    private final SubmissionService service;
    SubmissionController(SubmissionService s) { service = s; }

    /** Returns the message the student sees. The ONLY place where ExamException is finally handled. */
    String handle(String student, String course, String file, long sizeKb, LocalTime now) {
        try {
            service.submit(student, course, file, sizeKb, now);
            return "SUCCESS: " + file + " submitted";
        } catch (ExamWindowClosedException | NotRegisteredException | FileTooLargeException e) {
            return "REJECTED: " + e.getMessage();              // actionable message for the student
        } catch (SubmissionStorageException e) {
            System.out.println("   [log for admins] " + chain(e));   // full diagnostics go to the log, not to the student
            return "SORRY: your file could not be saved right now. Please try again in a few minutes.";
        } catch (ExamException e) {
            return "ERROR: " + e.getMessage();
        }
    }
    static String chain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable x = t; x != null; x = x.getCause()) {
            if (sb.length() > 0) sb.append("  <-caused by-  ");
            sb.append(x.getClass().getSimpleName()).append(": ").append(x.getMessage());
        }
        return sb.toString();
    }
}

public class ExamPortal {
    public static void main(String[] args) throws Exception {
        StorageLayer storage = new StorageLayer(500);
        Set<String> reg = new HashSet<>(List.of("S1:CSA09", "S2:CSA09"));
        SubmissionService service = new SubmissionService(LocalTime.of(9, 0), LocalTime.of(12, 0), reg, 200, storage);
        SubmissionController ctl = new SubmissionController(service);

        System.out.println("=== Q2 scenario table ===");
        Object[][] sc = {
            {"S1", "CSA09", "ans1.pdf", 120L, LocalTime.of(10, 0), "normal submission"},
            {"S1", "CSA09", "ans2.pdf", 120L, LocalTime.of(12, 30), "exam window closed"},
            {"S9", "CSA09", "ans3.pdf", 120L, LocalTime.of(10, 5), "student not registered"},
            {"S2", "CSA09", "ans4.pdf", 300L, LocalTime.of(10, 10), "file above 200 KB limit"},
            {"S2", "CSA09", "ans5.pdf", 150L, LocalTime.of(10, 15), "storage failure (quota forced to 100 KB below)"},
        };
        for (int i = 0; i < sc.length; i++) {
            SubmissionController c = ctl;
            if (i == 4) {                                       // tiny disk quota to force the storage failure
                c = new SubmissionController(new SubmissionService(LocalTime.of(9,0), LocalTime.of(12,0), reg, 200, new StorageLayer(100)));
            }
            String msg = c.handle((String) sc[i][0], (String) sc[i][1], (String) sc[i][2], (Long) sc[i][3], (LocalTime) sc[i][4]);
            System.out.printf("S%d %-48s -> %s%n", i + 1, sc[i][5], msg);
        }

        System.out.println("\n=== Unchecked configuration error (programmer bug) ===");
        try {
            new SubmissionService(LocalTime.of(12, 0), LocalTime.of(9, 0), reg, 200, storage);
        } catch (PortalConfigurationException e) {
            System.out.println("PortalConfigurationException (unchecked): " + e.getMessage());
        }

        System.out.println("\n=== Chained stack trace (cause preserved) ===");
        StorageLayer small = new StorageLayer(100);
        try { small.save("S2", "ans5.pdf", 150); }
        catch (SubmissionStorageException e) {
            e.printStackTrace(System.out);
            System.out.println("getCause() = " + e.getCause());
            System.out.println("frames in wrapper trace = " + e.getStackTrace().length
                             + ", frames in cause trace = " + e.getCause().getStackTrace().length);
        }

        System.out.println("\n=== Same failure, cause DROPPED ===");
        try { small.saveLossy("S2", "ans5.pdf", 150); }
        catch (SubmissionStorageException e) {
            e.printStackTrace(System.out);
            System.out.println("getCause() = " + e.getCause() + "   <- the quota reason and the original line number are lost");
        }
    }
}
