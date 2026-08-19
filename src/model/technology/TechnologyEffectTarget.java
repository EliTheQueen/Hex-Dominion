package model.technology;

public interface TechnologyEffectTarget extends java.io.Serializable {

    void enableSailing();

    boolean isSailingEnabled();

    void enableSteelTools();

    boolean isSteelToolsEnabled();

    void enableDefensiveArchitecture();

    boolean isDefensiveArchitectureEnabled();
}
