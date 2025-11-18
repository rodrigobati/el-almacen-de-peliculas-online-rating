package unrn.rating.messaging;

public class Event<K, T> {
    private EventType type;
    private K key;
    private T data;
    private String routingKey;

    static final String ERROR_TIPO_OBLIGATORIO = "El tipo del evento es obligatorio.";
    static final String ERROR_ROUTINGKEY_OBLIGATORIO = "La clave de enrutamiento es obligatoria.";

    public Event(EventType type, K key, T data, String routingKey) {
        this.type = type;
        this.key = key;
        this.data = data;
        this.routingKey = routingKey == null ? null : routingKey.trim();

        assertTipoPresente();
        assertRoutingKeyPresente();
    }

    private void assertTipoPresente() {
        if (this.type == null) {
            throw new RuntimeException(ERROR_TIPO_OBLIGATORIO);
        }
    }

    private void assertRoutingKeyPresente() {
        if (this.routingKey == null || this.routingKey.isBlank()) {
            throw new RuntimeException(ERROR_ROUTINGKEY_OBLIGATORIO);
        }
    }

    // Accesores con intención (no getters tontos)
    public EventType type() {
        return type;
    }

    public K key() {
        return key;
    }

    public T data() {
        return data;
    }

    public String routingKey() {
        return routingKey;
    }

}
