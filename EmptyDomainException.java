public class EmptyDomainException extends Exception {
    public EmptyDomainException() {
        this("Domain wipeout!");
    }

    public EmptyDomainException(String errorMessage) {
        super(errorMessage);
    }
}
