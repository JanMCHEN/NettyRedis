import java.lang.annotation.*;

public class AnnTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface A{
        String value();
    }

    @A("aa")
    static class B implements A {

        @Override
        public String value() {
            return null;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }

    public static void main(String[] args) {
        B b = new B();

        A a = B.class.getAnnotation(A.class);

        a.value();

        System.out.println(b);
        System.out.println(b.value());
    }
}
