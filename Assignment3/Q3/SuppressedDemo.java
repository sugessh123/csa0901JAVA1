/** Q3 Student B - closing order and suppressed exceptions. */
public class SuppressedDemo {
    static class Res implements AutoCloseable {
        final String name; final boolean failOnClose;
        Res(String name, boolean failOnClose) { this.name = name; this.failOnClose = failOnClose; System.out.println("  open  " + name); }
        @Override public void close() { System.out.println("  close " + name); if (failOnClose) throw new IllegalStateException("close failed: " + name); }
    }
    public static void main(String[] args) {
        System.out.println("--- 1. Closing order (declared r1, r2, r3) ---");
        try (Res r1 = new Res("r1", false); Res r2 = new Res("r2", false); Res r3 = new Res("r3", false)) {
            System.out.println("  body runs");
        }

        System.out.println("--- 2. try-with-resources: body fails AND two closes fail ---");
        try (Res a = new Res("A", true); Res b = new Res("B", true)) {
            throw new RuntimeException("body failed");
        } catch (RuntimeException e) {
            System.out.println("  primary   : " + e.getMessage());
            for (Throwable s : e.getSuppressed()) System.out.println("  suppressed: " + s.getMessage());
        }

        System.out.println("--- 3. manual finally: body fails AND close fails -> original is LOST ---");
        Res m = null;
        try {
            m = new Res("M", true);
            throw new RuntimeException("body failed");
        } catch (RuntimeException e) {
            System.out.println("  caught    : " + e.getMessage());
        } finally {
            try { if (m != null) m.close(); } catch (RuntimeException ce) { System.out.println("  finally threw: " + ce.getMessage()); }
        }
        System.out.println("--- 3b. same code without the inner try: exception from finally REPLACES the body exception ---");
        try {
            Res m2 = null;
            try { m2 = new Res("M2", true); throw new RuntimeException("body failed"); }
            finally { if (m2 != null) m2.close(); }
        } catch (RuntimeException e) {
            System.out.println("  caller sees: " + e.getMessage() + "  (suppressed count = " + e.getSuppressed().length + ")");
        }
    }
}
