package model.technology;

//هدفش این است که اثر دائمی هر تکنولوژی را از خود Registry جدا کند. Registry فقط می‌گوید چه چیزی تحقیق شده؛ Effect می‌گوید آن تحقیق چه تغییری در Empire ایجاد می‌کند.
public interface TechnologyEffect {

    TechnologyType getTechnologyType();

     void apply(TechnologyEffectTarget target);
}
