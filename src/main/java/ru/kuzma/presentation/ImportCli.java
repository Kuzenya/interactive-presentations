package ru.kuzma.presentation;

import ru.kuzma.presentation.importing.GlbImportService;
import ru.kuzma.presentation.storage.JsonSceneRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Импорт: java -jar target/interactive-presentations-1.0.0.jar --import model.glb data/scene.json */
public final class ImportCli {
    private ImportCli() { }

    public static void run(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Использование: --import <model.glb> <data/scene.json>");
        }
        Path input = Path.of(args[1]).toAbsolutePath().normalize();
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(input)) throw new IllegalArgumentException("GLB-файл не найден: " + input);
        Path dataDirectory = output.getParent();
        if (dataDirectory == null) throw new IllegalArgumentException("Укажите выходной JSON внутри каталога data");
        Path modelsDirectory = dataDirectory.resolve("models");
        Files.createDirectories(modelsDirectory);
        Path storedModel = modelsDirectory.resolve(input.getFileName().toString());
        if (!input.equals(storedModel.toAbsolutePath().normalize())) {
            Files.copy(input, storedModel, StandardCopyOption.REPLACE_EXISTING);
        }
        String relativeModelPath = dataDirectory.relativize(storedModel).toString().replace('\\', '/');
        GlbImportService.Result result = new GlbImportService().importGlb(input, null, relativeModelPath);
        new JsonSceneRepository().save(output, result.scene());
        System.out.printf("Импорт завершен: узлов=%d, mesh=%d, анимаций=%d%nСцена: %s%n",
                result.nodeCount(), result.meshCount(), result.animationCount(), output);
    }
}
