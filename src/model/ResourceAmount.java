package model;

import java.util.EnumMap;
import java.util.Map;
import static model.Constants.ResourceType;

public class ResourceAmount implements java.io.Serializable {
    private final EnumMap<ResourceType,Integer> values = new EnumMap<>(ResourceType.class);

    public ResourceAmount(){
        for(ResourceType r : ResourceType.values())
            values.put(r,0);
    }

    public static ResourceAmount zero() { return new ResourceAmount(); }

    public static ResourceAmount of(int food,int wood,int stone,int iron) {
        ResourceAmount a = new ResourceAmount();
        a.set(ResourceType.FOOD,food);
        a.set(ResourceType.WOOD,wood);
        a.set(ResourceType.STONE,stone);
        a.set(ResourceType.IRON,iron);
        return a;
    }

    public int get(ResourceType r) { return values.getOrDefault(r,0); }
    public void set(ResourceType r,int v) { values.put(r,Math.max(0,v)); }

    public ResourceAmount add(ResourceAmount o) {
        ResourceAmount n = copy();
        for(ResourceType r : ResourceType.values())
            n.set(r,n.get(r)+o.get(r));
        return n;
    }

    public ResourceAmount subtract(ResourceAmount o) {
        ResourceAmount n = copy();
        for(ResourceType r : ResourceType.values())
            n.values.put(r,n.get(r)-o.get(r));
        return n;
    }

    public ResourceAmount multiply(double m){
        ResourceAmount n = new ResourceAmount();
        for(ResourceType r : ResourceType.values())
            n.set(r,(int)Math.floor(get(r)*m));
        return n;
    }

    public boolean isNegative() {
        for (ResourceType r : ResourceType.values())
            if(get(r)<0)
                return true;
        return false;
    }
    public boolean hasEnough(ResourceAmount cost) {
        for(ResourceType r : ResourceType.values())
            if(get(r)<cost.get(r))
                return false;
        return true;
    }

    public ResourceAmount copy(){
        ResourceAmount n = new ResourceAmount();
        for(Map.Entry<ResourceType,Integer> e : values.entrySet())
            n.values.put(e.getKey(),e.getValue());
        return n;
    }
}
