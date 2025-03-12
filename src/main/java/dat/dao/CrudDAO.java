package dat.dao;

import java.util.List;

public interface CrudDAO
{
    <T> T create(T object);
    <T> List<T> create(List<T> objects);

    <T> T update(T object);
    <T> List<T> update(List<T> objects);

    <T> T read(Class<T> type, Long id);

    <T> List<T> findAll(Class<T> type);

    <T> void delete(T object);

    <T> void delete(Class<T> type, Long id);
}
