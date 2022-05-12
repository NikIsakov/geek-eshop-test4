package ru.geekbrains.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.geekbrains.persist.PictureRepository;
import ru.geekbrains.persist.model.Picture;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
public class PictureServiceImpl implements PictureService {

    private static final Logger logger = LoggerFactory.getLogger(PictureServiceImpl.class);

    @Value("${picture.storage.path}")
    private String storagePath;

    private final PictureRepository pictureRepository;

    @Autowired
    public PictureServiceImpl(PictureRepository pictureRepository) {
        this.pictureRepository = pictureRepository;
    }

    @Override
    public Optional<String> getPictureContentType(long id) {
        return pictureRepository.findById(id).map(Picture::getContentType);
    }

    @Override
    public Optional<byte[]> getPictureDataById(long id) {
        return pictureRepository.findById(id)
                .map(pic -> Paths.get(storagePath, pic.getStorageFileName()))
                .filter(Files::exists)
                .map(path -> {
                    try {
                        return Files.readAllBytes(path);
                    } catch (IOException ex) {
                        logger.error("Can't read file", ex);
                        throw new RuntimeException(ex);
                    }
                });
    }

    @Override
    public String createPicture(byte[] pictureData) {
        String filename = UUID.randomUUID().toString();
        try (OutputStream os = Files.newOutputStream(Paths.get(storagePath, filename))) {
            os.write(pictureData);
        } catch (IOException ex) {
            logger.error("Can't write to file", ex);
            throw new RuntimeException(ex);
        }
        return filename;
    }
}
