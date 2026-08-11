package com.travelapp.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class LocationCatalog {

    private final String name;
    private final String city;
    private final String state;
    private final String country;
    private final String description;
    private final String imageUrl;
    private final double latitude;
    private final double longitude;
    private final String tags;

    public String[] searchTokens() {
        return (name + " " + city + " " + state + " " + country + " " + tags)
                .toLowerCase()
                .split("[\\s,]+");
    }

    public static List<LocationCatalog> all() {
        return ENTRIES;
    }

    public static List<LocationCatalog> matchQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }
        List<String> terms = Arrays.stream(rawQuery.toLowerCase().split("[,]+"))
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .flatMap(t -> Arrays.stream(t.split("\\s+")))
                .filter(t -> t.length() > 1)
                .distinct()
                .toList();

        if (terms.isEmpty()) {
            return List.of();
        }

        return ENTRIES.stream()
                .filter(entry -> terms.stream().allMatch(term -> entry.matchesTerm(term)))
                .collect(Collectors.toList());
    }

    private boolean matchesTerm(String term) {
        String blob = (name + " " + city + " " + state + " " + country + " " + tags).toLowerCase();
        if (blob.contains(term)) {
            return true;
        }
        // "andhra" matches "andhra pradesh" when user types partial state
        for (String token : searchTokens()) {
            if (token.startsWith(term) || term.startsWith(token)) {
                return true;
            }
        }
        return false;
    }

    private static final List<LocationCatalog> ENTRIES = List.of(
            entry("Hyderabad", "Hyderabad", "Andhra Pradesh", "India",
                    "Pearl City — biryani, Charminar, and thriving tech culture.",
                    "https://images.unsplash.com/featured/800x600/?hyderabad,travel,landmark", 17.3850, 78.4867,
                    "hyderabad,andhra pradesh,telangana,biryani,india"),
            entry("Visakhapatnam", "Visakhapatnam", "Andhra Pradesh", "India",
                    "Coastal gem with beaches, hills, and seafood.",
                    "https://images.unsplash.com/featured/800x600/?visakhapatnam,beach", 17.6868, 83.2185,
                    "vizag,visakhapatnam,andhra pradesh,beach,india"),
            entry("Vijayawada", "Vijayawada", "Andhra Pradesh", "India",
                    "Ancient city on the Krishna river with rich Andhra cuisine.",
                    "https://images.unsplash.com/featured/800x600/?vijayawada,india", 16.5062, 80.6480,
                    "vijayawada,andhra pradesh,india"),
            entry("Bangalore", "Bengaluru", "Karnataka", "India",
                    "Garden City — startups, breweries, and South Indian flavors.",
                    "https://images.unsplash.com/featured/800x600/?bangalore,city", 12.9716, 77.5946,
                    "bangalore,bengaluru,karnataka,india"),
            entry("Mumbai", "Mumbai", "Maharashtra", "India",
                    "Maximum city — street food, Bollywood, and marine drive sunsets.",
                    "https://images.unsplash.com/featured/800x600/?mumbai,india", 19.0760, 72.8777,
                    "mumbai,bombay,maharashtra,india"),
            entry("Delhi", "New Delhi", "Delhi", "India",
                    "Historic capital with Mughlai cuisine and vibrant markets.",
                    "https://images.unsplash.com/featured/800x600/?delhi,landmark", 28.6139, 77.2090,
                    "delhi,new delhi,india"),
            entry("Chennai", "Chennai", "Tamil Nadu", "India",
                    "Gateway to South India — filter coffee, temples, and Marina Beach.",
                    "https://images.unsplash.com/featured/800x600/?chennai,india", 13.0827, 80.2707,
                    "chennai,madras,tamil nadu,india"),
            entry("Kolkata", "Kolkata", "West Bengal", "India",
                    "City of Joy — rosogolla, trams, and colonial architecture.",
                    "https://images.unsplash.com/featured/800x600/?kolkata,city", 22.5726, 88.3639,
                    "kolkata,calcutta,west bengal,india"),
            entry("Jaipur", "Jaipur", "Rajasthan", "India",
                    "Pink City with palaces, bazaars, and royal Rajasthani thalis.",
                    "https://images.unsplash.com/featured/800x600/?jaipur,palace", 26.9124, 75.7873,
                    "jaipur,rajasthan,india"),
            entry("Goa", "Panaji", "Goa", "India",
                    "Beaches, seafood shacks, and Portuguese-Indian fusion.",
                    "https://images.unsplash.com/featured/800x600/?goa,beach", 15.4909, 73.8278,
                    "goa,panaji,beach,india"),
            entry("Pune", "Pune", "Maharashtra", "India",
                    "Oxford of the East with misal pav and pleasant weather.",
                    "https://images.unsplash.com/featured/800x600/?pune,india", 18.5204, 73.8567,
                    "pune,maharashtra,india"),
            entry("Kochi", "Kochi", "Kerala", "India",
                    "Backwaters, spice trade history, and Kerala seafood.",
                    "https://images.unsplash.com/featured/800x600/?kochi,kerala", 9.9312, 76.2673,
                    "kochi,kerala,india"),
            entry("Paris", "Paris", "Île-de-France", "France",
                    "The City of Light — iconic landmarks and world-class cuisine.",
                    "https://images.unsplash.com/featured/800x600/?paris,eiffel", 48.8566, 2.3522,
                    "paris,france,europe"),
            entry("Tokyo", "Tokyo", "Kanto", "Japan",
                    "Ultramodern metropolis with temples and legendary sushi.",
                    "https://images.unsplash.com/featured/800x600/?tokyo,japan", 35.6762, 139.6503,
                    "tokyo,japan,asia"),
            entry("Switzerland", "Zurich", "Zurich", "Switzerland",
                    "Breathtaking Alpine landscapes, pristine lakes, and world-class chocolates.",
                    "https://images.unsplash.com/featured/800x600/?switzerland,alps", 47.3769, 8.5417,
                    "switzerland,swiss,zurich,europe")
    );

    private static LocationCatalog entry(String name, String city, String state, String country,
                                         String description, String imageUrl,
                                         double lat, double lng, String tags) {
        return LocationCatalog.builder()
                .name(name)
                .city(city)
                .state(state)
                .country(country)
                .description(description)
                .imageUrl(imageUrl)
                .latitude(lat)
                .longitude(lng)
                .tags(tags)
                .build();
    }
}
