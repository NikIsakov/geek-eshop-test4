package ru.geekbrains.service;

import java.util.Optional;

public interface PictureService {

    Optional<String> getPictureContentType(long id);

    Optional<byte[]> getPictureDataById(long id);

    String createPicture(byte[] pictureData);
}
