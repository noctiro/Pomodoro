package org.encinet.pomodoro.config.utils;

import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A utility class to handle type conversions for configuration values.
 * Provides flexible and safe casting from configuration objects to Java field types.
 * This version uses a strategy pattern for extensibility and supports Lists.
 */
public final class TypeConverter {

    // Strategy pattern: A map of functions to handle conversions to specific types.
    private static final Map<Class<?>, Function<Object, Object>> CONVERTERS = new HashMap<>();

    static {
        // String-to-Type converters
        CONVERTERS.put(String.class, Object::toString);
        CONVERTERS.put(Integer.class, obj -> tryParse(obj.toString(), Integer::parseInt));
        CONVERTERS.put(int.class, obj -> tryParse(obj.toString(), Integer::parseInt));
        CONVERTERS.put(Double.class, obj -> tryParse(obj.toString(), Double::parseDouble));
        CONVERTERS.put(double.class, obj -> tryParse(obj.toString(), Double::parseDouble));
        CONVERTERS.put(Boolean.class, obj -> Boolean.parseBoolean(obj.toString()));
        CONVERTERS.put(boolean.class, obj -> Boolean.parseBoolean(obj.toString()));
        CONVERTERS.put(Long.class, obj -> tryParse(obj.toString(), Long::parseLong));
        CONVERTERS.put(long.class, obj -> tryParse(obj.toString(), Long::parseLong));
        CONVERTERS.put(Float.class, obj -> tryParse(obj.toString(), Float::parseFloat));
        CONVERTERS.put(float.class, obj -> tryParse(obj.toString(), Float::parseFloat));
        CONVERTERS.put(Short.class, obj -> tryParse(obj.toString(), Short::parseShort));
        CONVERTERS.put(short.class, obj -> tryParse(obj.toString(), Short::parseShort));
        CONVERTERS.put(Byte.class, obj -> tryParse(obj.toString(), Byte::parseByte));
        CONVERTERS.put(byte.class, obj -> tryParse(obj.toString(), Byte::parseByte));
    }

    private TypeConverter() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts a raw object from a configuration file to a specific target type.
     *
     * @param value       The raw value from the configuration.
     * @param targetType  The target class of the field.
     * @param genericType The generic type of the field, used for collections like Maps and Lists.
     * @return The converted object, or null if conversion is not possible.
     */
    public static Object convert(Object value, Class<?> targetType, Type genericType) {
        if (value == null) return null;

        // 1. If types are already compatible, no conversion needed
        if (targetType.isInstance(value)) return value;

        // 2. Handle automatic, case-insensitive String-to-Enum conversion
        if (targetType.isEnum()) {
            try {
                // This allows users to write "easy", "EASY", or "Easy" in the config
                return Enum.valueOf((Class<Enum>) targetType, value.toString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null; // The string doesn't match any enum constant
            }
        }

        // 3. Prioritize direct, efficient number-to-number conversion
        if (value instanceof Number && (Number.class.isAssignableFrom(targetType) || targetType.isPrimitive())) {
            Object convertedNumber = convertNumber((Number) value, targetType);
            if (convertedNumber != null) return convertedNumber;
        }

        // 4. Use the flexible strategy map for lenient conversions (e.g., "123" -> 123)
        Function<Object, Object> converter = CONVERTERS.get(targetType);
        if (converter != null) {
            return converter.apply(value);
        }

        // 5. Handle complex types like Maps and Collections
        if (value instanceof ConfigurationSection && Map.class.isAssignableFrom(targetType)) {
            return convertSectionToMap((ConfigurationSection) value, (ParameterizedType) genericType);
        }
        if (value instanceof List && Collection.class.isAssignableFrom(targetType)) {
            return convertListToCollection((List<?>) value, (ParameterizedType) genericType);
        }

        return null; // Or log a warning if conversion is not supported
    }

    /**
     * Handles direct conversion between different Number types.
     * @return The converted number, or null if the target type is not a known number type.
     */
    private static Object convertNumber(Number value, Class<?> targetType) {
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) return value.intValue();
        if (Double.class.equals(targetType) || double.class.equals(targetType)) return value.doubleValue();
        if (Long.class.equals(targetType) || long.class.equals(targetType)) return value.longValue();
        if (Float.class.equals(targetType) || float.class.equals(targetType)) return value.floatValue();
        if (Short.class.equals(targetType) || short.class.equals(targetType)) return value.shortValue();
        if (Byte.class.equals(targetType) || byte.class.equals(targetType)) return value.byteValue();
        return null;
    }

    private static Map<String, ?> convertSectionToMap(ConfigurationSection section, ParameterizedType mapType) {
        Type[] typeArguments = mapType.getActualTypeArguments();
        if (typeArguments.length < 2) return new HashMap<>(); // Not a valid map type

        Type valueGenericType = typeArguments[1];
        Class<?> valueClass = getClassFromType(valueGenericType);
        if (valueClass == null) return new HashMap<>();

        Map<String, Object> newMap = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object rawValue = section.get(key);
            Object convertedValue = convert(rawValue, valueClass, valueGenericType);
            if (convertedValue != null) {
                newMap.put(key, convertedValue);
            }
        }
        return newMap;
    }

    private static Collection<?> convertListToCollection(List<?> rawList, ParameterizedType collectionType) {
        Type[] typeArguments = collectionType.getActualTypeArguments();
        if (typeArguments.length < 1) return new ArrayList<>(); // Not a valid collection type

        Type elementGenericType = typeArguments[0];
        Class<?> elementClass = getClassFromType(elementGenericType);
        if (elementClass == null) return new ArrayList<>();

        List<Object> newList = rawList.stream()
                .map(item -> convert(item, elementClass, elementGenericType))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Support converting to a Set as well as a List
        if (Set.class.isAssignableFrom((Class<?>) collectionType.getRawType())) {
            return new HashSet<>(newList);
        }
        return newList;
    }

    private static Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        return null;
    }

    private static <T> T tryParse(String s, Function<String, T> parser) {
        try {
            return parser.apply(s);
        } catch (NumberFormatException e) {
            return null; // Return null if parsing fails, caller should handle this.
        }
    }
}
