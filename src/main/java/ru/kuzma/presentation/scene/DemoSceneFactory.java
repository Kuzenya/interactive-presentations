package ru.kuzma.presentation.scene;

import ru.kuzma.presentation.model.*;

import java.util.List;

/** Создаёт самодостаточную демонстрационную сцену с электродвигателем. */
public final class DemoSceneFactory {
    public SceneData create() {
        SceneData scene = new SceneData();
        scene.setId("demo-electric-motor");
        scene.setName("Интерактивная презентация электродвигателя");
        scene.setBackgroundColor("#25313D");

        SceneObjectData motor = object("motor", null, "Электродвигатель", SceneObjectType.GROUP);
        motor.setSourceNodeKey("ElectricMotor");
        motor.setTransform(new TransformData(new Vector3Data(0, 0.32f, 0), QuaternionData.identity(), new Vector3Data(1.2f, 1.2f, 1.2f)));
        motor.setDescriptionMarkdown("# Электродвигатель\n\nНажмите на компонент, чтобы увидеть описание. Клавиша **E** запускает событие.");
        motor.setImagePaths(List.of("images/motor.png"));

        SceneObjectData stator = object("stator", "motor", "Корпус статора", SceneObjectType.MESH);
        stator.setSourceNodeKey("StatorHousing");
        stator.setDescriptionMarkdown("## Корпус статора\n\nНесущая деталь. Внутри размещаются статор и обмотки; ребра улучшают теплоотвод.");

        SceneObjectData shaft = object("shaft", "motor", "Передний щит и вал", SceneObjectType.MESH);
        shaft.setSourceNodeKey("FrontShieldAndShaft");
        shaft.setDescriptionMarkdown("## Передний щит и вал\n\nЩит фиксирует подшипниковый узел. Вал передает вращающий момент внешнему механизму.");

        SceneObjectData fan = object("fan", "motor", "Задняя крышка вентилятора", SceneObjectType.MESH);
        fan.setSourceNodeKey("RearFanCover");
        fan.setDescriptionMarkdown("## Задняя крышка вентилятора\n\nЗащищает вентилятор и направляет поток охлаждающего воздуха вдоль корпуса.");

        scene.setObjects(List.of(motor, stator, shaft, fan));
        scene.setModels(List.of(new ModelResourceData("motor-glb", "Электродвигатель", "models/electric_motor.glb", "motor")));

        EventData inspection = new EventData("inspection", "motor", "Демонстрация работы и разборки");
        inspection.setAnimations(List.of(
                new AnimationSpec("shaft", AnimationChannel.ROTATE_X, (float) (Math.PI * 4), 2.0f),
                new AnimationSpec("fan", AnimationChannel.TRANSLATE_Y, 0.65f, 2.8f)
        ));
        scene.setEvents(List.of(inspection));
        return scene;
    }

    private static SceneObjectData object(String id, String parent, String name, SceneObjectType type) {
        return new SceneObjectData(id, parent, name, type);
    }
}
