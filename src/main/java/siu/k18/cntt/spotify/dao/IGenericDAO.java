package siu.k18.cntt.spotify.dao;

import java.util.List;

public interface IGenericDAO<T> {
    List<T> getTracks(int offset, int limit, String searchCol, String keyword);
    int getTotalTracks(String searchCol, String keyword);
    boolean insert(T obj);
    boolean update(T obj);
    boolean delete(String id);
}