package matchdog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by marekful on 15/02/2016.
 */
public class Prefs {

    public void initProps(Properties props) throws IllegalArgumentException {

        for(Object key : props.keySet()) {
            try {
                String[] split = key.toString().split("_");
                if(split.length < 2) {
                    throw new IllegalArgumentException("Key name: " + key.toString() + " is not valid.");
                }
                String keyName = split[1];
                String valueType = split[0];
                int serial = split.length > 2 ? Integer.parseInt(split[2]) : 0;
                Object value = null;
                String accessType = "primitive";

                if(valueType.equals("as")) {
                    accessType = "arrayString";
                    valueType = "s";
                }
                else if(valueType.equals("msi")) {
                    accessType = "mapStringInteger";
                    valueType = "s";
                }
                else if(valueType.equals("als")) {
                    accessType = "arrayListString";
                    valueType = "s";
                }

                switch (valueType) {
                    case "s":
                        value = props.get(key);
                        break;
                    case "i":
                        value = Integer.parseInt((String) props.get(key));
                        break;
                    case "b":
                        value = Boolean.parseBoolean((String) props.get(key));
                        break;
                    case "d":
                        value = Double.parseDouble((String) props.get(key));
                        break;
                }

                Field field = null;
                try {
                    field = PlayerPrefs.class.getField(keyName);
                } catch (NoSuchFieldException e) {}

                if(field == null) {
                    field = ProgramPrefs.class.getField(keyName);
                }

                switch (accessType) {
                    case "arrayString" :
                        String[] s = (String[]) field.get(this);
                        s[serial] = (String)value;
                        break;
                    case "arrayListString" :
                        ((ArrayList<String>) field.get(this)).add((String)value);
                        break;
                    case "mapStringInteger" :
                        Map<String, Integer> m;
                        m = ((HashMap<String, Integer>) field.get(this));
                        String [] val = value.toString().split(" ");
                        m.put(val[0], Integer.parseInt(val[1]));
                        break;
                    case "primitive" :
                        field.set(this, value);
                }

                //System.out.println(">> >> " + keyName + " --> " + this.props.get(key) + " --> " + value);

            }
            catch(NoSuchFieldException | IllegalAccessException e) { e.printStackTrace(); }
        }
    }
}
