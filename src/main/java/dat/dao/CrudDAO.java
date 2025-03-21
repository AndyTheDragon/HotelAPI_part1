package dat.dao;

import java.util.List;

public interface CrudDAO
{
    <T> T create(T object);
    <T> List<T> create(List<T> objects);

    <T> T getById(Class<T> type, Object id);
    <T> List<T> getAll(Class<T> type);

    <T> T update(T object);
    <T> List<T> update(List<T> objects);

    <T> void delete(T object);
    <T> void delete(Class<T> type, Object id);
}
