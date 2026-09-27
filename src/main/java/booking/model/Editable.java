package booking.model;

import java.util.List;

/**
 * Контракт «сущность можно менять».
 * Кнопка «Изменить» в GUI доступна только для объектов, реализующих этот интерфейс.
 */
public interface Editable {
    /** @return список ошибок; пустой список = данные корректны */
    List<String> validate();
}
