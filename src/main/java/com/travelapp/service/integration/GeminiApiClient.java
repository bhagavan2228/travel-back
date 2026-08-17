package com.travelapp.service.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiApiClient {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callGemini(String prompt) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/gemini-3.6-flash:generateContent")
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            return extractTextFromResponse(response);

        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            throw new RuntimeException("Gemini API call failed", e);
        }
    }

    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse text from Gemini response", e);
        }
        return "";
    }

    public List<Map<String, String>> extractEventsFromNews(String newsContext, String city) {
        try {
            String prompt = "You are an expert travel guide. Based on recent travel articles and local news for " + city + ", " +
                    "extract or suggest the TOP 10 best places to visit in " + city + ". " +
                    "For each place, provide: " +
                    "1. 'title' (name of the attraction/place) " +
                    "2. 'bestTime' (best time of day/season to cover it, e.g., 'Morning (8:00 AM - 11:00 AM)' or 'Evening (5:00 PM - 8:00 PM)') " +
                    "3. 'category' (e.g., 'Historic Landmark', 'Museum', 'Scenic Park', 'Shopping') " +
                    "4. 'description' (a brief explanation of what to see/do there, max 150 chars). " +
                    "Return ONLY a JSON array of objects. Do not wrap in markdown or any other text.";

            String jsonText = callGemini(prompt);
            if (jsonText != null) {
                jsonText = jsonText.replace("```json", "").replace("```", "").trim();
                int startIdx = jsonText.indexOf('[');
                int endIdx = jsonText.lastIndexOf(']');
                if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                    jsonText = jsonText.substring(startIdx, endIdx + 1);
                }
                return objectMapper.readValue(jsonText, new TypeReference<List<Map<String, String>>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse events with Gemini API, using fallback", e);
        }
        return generateFallbacks(city);
    }

    private List<Map<String, String>> generateFallbacks(String city) {
        String cleanCity = city.trim().toLowerCase();
        if (cleanCity.contains("paris")) {
            return List.of(
                Map.of("title", "Eiffel Tower", "bestTime", "Evening (6:00 PM - 10:00 PM)", "category", "Iconic Monument", "description", "Witness the breathtaking sparkling lights of the world's most famous tower at night."),
                Map.of("title", "Louvre Museum", "bestTime", "Morning (9:00 AM - 12:30 PM)", "category", "Art Museum", "description", "Explore priceless treasures including the Mona Lisa and Venus de Milo in a historic palace."),
                Map.of("title", "Notre-Dame Cathedral", "bestTime", "Afternoon (2:00 PM - 4:30 PM)", "category", "Historic Cathedral", "description", "Admire the brilliant French Gothic architecture and stunning stained-glass windows."),
                Map.of("title", "Arc de Triomphe", "bestTime", "Sunset (5:30 PM - 7:00 PM)", "category", "Historic Landmark", "description", "Climb to the top for magnificent panoramic views of the Champs-Élysées and Parisian avenues."),
                Map.of("title", "Sacré-Cœur & Montmartre", "bestTime", "Late Afternoon (4:00 PM - 6:00 PM)", "category", "Scenic Neighborhood", "description", "Stroll through the historic artists' quarter and enjoy sweeping hilltop views from the Basilica."),
                Map.of("title", "Seine River Cruise", "bestTime", "Night (8:00 PM - 9:30 PM)", "category", "Boat Tour", "description", "Glide past illuminated monuments like the Musée d'Orsay and Louvre on a scenic boat."),
                Map.of("title", "Palace of Versailles", "bestTime", "Morning (8:30 AM - 1:00 PM)", "category", "Royal Palace", "description", "Tour the opulent Hall of Mirrors and wander through the vast, immaculate gardens."),
                Map.of("title", "Musée d'Orsay", "bestTime", "Afternoon (1:30 PM - 4:30 PM)", "category", "Art Museum", "description", "Marvel at the world's largest collection of Impressionist masterpieces in a grand old railway station."),
                Map.of("title", "Jardin des Tuileries", "bestTime", "Midday (12:00 PM - 2:00 PM)", "category", "Scenic Park", "description", "Relax by the beautiful fountains and walk through historic gardens designed by André Le Nôtre."),
                Map.of("title", "Champs-Élysées", "bestTime", "Evening (5:00 PM - 8:00 PM)", "category", "Shopping Street", "description", "Stroll along one of the world's most famous avenues lined with luxury boutiques and cafes.")
            );
        } else if (cleanCity.contains("tokyo")) {
            return List.of(
                Map.of("title", "Senso-ji Temple", "bestTime", "Morning (8:00 AM - 10:30 AM)", "category", "Historic Temple", "description", "Visit Tokyo's oldest and most iconic Buddhist temple in the historic Asakusa district."),
                Map.of("title", "Shibuya Crossing", "bestTime", "Night (7:00 PM - 10:00 PM)", "category", "Iconic Landmark", "description", "Experience the famous, dizzying scramble crossing surrounded by giant neon screens."),
                Map.of("title", "Meiji Shrine", "bestTime", "Early Morning (6:30 AM - 8:30 AM)", "category", "Shinto Shrine", "description", "Find peace in a dense forest sanctuary dedicated to Emperor Meiji and Empress Shoken."),
                Map.of("title", "Tokyo Skytree", "bestTime", "Sunset (5:00 PM - 7:00 PM)", "category", "Observation Tower", "description", "Enjoy breathtaking 360-degree views of Tokyo and Mount Fuji from Japan's tallest tower."),
                Map.of("title", "Shinjuku Gyoen National Garden", "bestTime", "Midday (11:00 AM - 1:30 PM)", "category", "Botanical Garden", "description", "Walk through English, French, and traditional Japanese landscape gardens."),
                Map.of("title", "Akihabara Electric Town", "bestTime", "Afternoon (2:00 PM - 5:30 PM)", "category", "Shopping District", "description", "Explore Tokyo's hub for electronics, anime, gaming, and unique pop culture."),
                Map.of("title", "Tsukiji Outer Market", "bestTime", "Morning (7:30 AM - 10:30 AM)", "category", "Food Market", "description", "Sample fresh sushi, grilled seafood, and delicious Japanese street food specialties."),
                Map.of("title", "Imperial Palace East Gardens", "bestTime", "Morning (9:30 AM - 11:30 AM)", "category", "Historic Gardens", "description", "Walk along the castle moats, stone walls, and ruins of the ancient Edo Castle."),
                Map.of("title", "Takeshita Street (Harajuku)", "bestTime", "Afternoon (1:00 PM - 4:00 PM)", "category", "Shopping Street", "description", "Immerse yourself in colorful teenage fashion, trendy boutiques, and giant crepes."),
                Map.of("title", "teamLab Planets", "bestTime", "Evening (6:00 PM - 8:30 PM)", "category", "Digital Art Museum", "description", "Walk through water and interact with immersive, sensory-exploding digital art displays.")
            );
        } else if (cleanCity.contains("new york")) {
            return List.of(
                Map.of("title", "Statue of Liberty & Ellis Island", "bestTime", "Morning (8:30 AM - 12:00 PM)", "category", "Historic Monument", "description", "Take the ferry to visit America's symbol of freedom and explore the historic immigration museum."),
                Map.of("title", "Central Park", "bestTime", "Midday (11:00 AM - 2:00 PM)", "category", "Scenic Park", "description", "Rent a bike, row a boat, or picnic in the world's most famous urban green oasis."),
                Map.of("title", "Empire State Building", "bestTime", "Sunset (5:30 PM - 7:30 PM)", "category", "Observation Deck", "description", "Marvel at the spectacular city skyline views from the iconic Art Deco skyscraper."),
                Map.of("title", "Times Square", "bestTime", "Night (8:00 PM - 11:00 PM)", "category", "Iconic Landmark", "description", "Bask in the dazzling glow of neon billboards at the crossroad of the world."),
                Map.of("title", "Brooklyn Bridge Walkway", "bestTime", "Early Morning (6:00 AM - 8:00 AM)", "category", "Historic Bridge", "description", "Walk across the East River for unmatched skyline photos of Manhattan and Brooklyn."),
                Map.of("title", "Metropolitan Museum of Art", "bestTime", "Afternoon (1:30 PM - 5:00 PM)", "category", "Art Museum", "description", "Journey through 5,000 years of global art in one of the world's greatest museums."),
                Map.of("title", "Broadway Theatre District", "bestTime", "Night (7:00 PM - 10:30 PM)", "category", "Theatre", "description", "Watch a world-class musical or dramatic play in the heart of NYC's entertainment hub."),
                Map.of("title", "The High Line", "bestTime", "Late Afternoon (4:00 PM - 6:00 PM)", "category", "Elevated Park", "description", "Stroll along an old elevated railway line transformed into a beautiful public park."),
                Map.of("title", "9/11 Memorial & Museum", "bestTime", "Morning (9:30 AM - 12:00 PM)", "category", "Historic Memorial", "description", "Pay respects at the reflection pools and learn about the historic events of Sept 11."),
                Map.of("title", "Rockefeller Center & Top of the Rock", "bestTime", "Evening (5:00 PM - 7:30 PM)", "category", "Observation Deck", "description", "Enjoy stunning vistas of Central Park and the Empire State Building from the open-air deck.")
            );
        } else if (cleanCity.contains("london")) {
            return List.of(
                Map.of("title", "British Museum", "bestTime", "Morning (10:00 AM - 12:30 PM)", "category", "History Museum", "description", "Explore human history and culture, featuring the Rosetta Stone and Egyptian mummies."),
                Map.of("title", "Tower of London", "bestTime", "Morning (9:00 AM - 11:30 AM)", "category", "Historic Fortress", "description", "Discover a 1,000-year-old royal fortress and view the magnificent Crown Jewels."),
                Map.of("title", "Big Ben & Palace of Westminster", "bestTime", "Midday (12:00 PM - 1:30 PM)", "category", "Historic Landmark", "description", "Admire the iconic Gothic Revival clock tower and the home of the UK Parliament."),
                Map.of("title", "London Eye", "bestTime", "Sunset (5:30 PM - 7:00 PM)", "category", "Scenic Ride", "description", "Enjoy a slow, breathtaking flight over the River Thames with panoramic city views."),
                Map.of("title", "Buckingham Palace", "bestTime", "Morning (10:30 AM - 11:30 AM)", "category", "Royal Palace", "description", "Watch the iconic Changing of the Guard ceremony at the King's official residence."),
                Map.of("title", "Hyde Park", "bestTime", "Afternoon (2:00 PM - 4:00 PM)", "category", "Scenic Park", "description", "Wander through Royal Park gardens, rent a pedal boat, or visit Kensington Palace."),
                Map.of("title", "West End Theatre District", "bestTime", "Night (7:00 PM - 10:00 PM)", "category", "Theatre", "description", "Attend a legendary musical or drama in London's premier theatre district."),
                Map.of("title", "Tower Bridge", "bestTime", "Sunset (6:00 PM - 7:30 PM)", "category", "Historic Bridge", "description", "Walk along the high-level glass walkways for views of the Thames and city life."),
                Map.of("title", "Westminster Abbey", "bestTime", "Morning (9:30 AM - 11:30 AM)", "category", "Historic Church", "description", "Visit the coronation church of British monarchs and final resting place of historical icons."),
                Map.of("title", "Covent Garden", "bestTime", "Evening (5:00 PM - 8:00 PM)", "category", "Shopping & Dining", "description", "Watch street performers, shop local crafts, and enjoy lively indoor market cafes.")
            );
        } else if (cleanCity.contains("dubai")) {
            return List.of(
                Map.of("title", "Burj Khalifa", "bestTime", "Sunset (5:00 PM - 7:00 PM)", "category", "Observation Deck", "description", "Look down from the world's tallest building for a stunning view of city and desert."),
                Map.of("title", "Dubai Fountain & Dubai Mall", "bestTime", "Evening (6:30 PM - 9:00 PM)", "category", "Shopping & Show", "description", "Watch the grand choreographed music fountain show outside the world's largest mall."),
                Map.of("title", "Palm Jumeirah & Atlantis", "bestTime", "Afternoon (1:30 PM - 4:30 PM)", "category", "Iconic Landmark", "description", "Explore the world's largest man-made island, water parks, and luxury resorts."),
                Map.of("title", "Desert Safari & Dune Bashing", "bestTime", "Late Afternoon (3:30 PM - 9:00 PM)", "category", "Adventure", "description", "Ride over golden sand dunes, ride camels, and enjoy a traditional Bedouin dinner."),
                Map.of("title", "Dubai Marina Walk", "bestTime", "Night (7:30 PM - 10:00 PM)", "category", "Scenic Walkway", "description", "Stroll along the waterfront promenade framed by illuminated skyscrapers and superyachts."),
                Map.of("title", "Gold & Spice Souks", "bestTime", "Morning (9:30 AM - 11:30 AM)", "category", "Traditional Market", "description", "Wander historical alleys filled with glistening gold jewelry and exotic spices."),
                Map.of("title", "Ski Dubai", "bestTime", "Midday (11:00 AM - 2:00 PM)", "category", "Indoor Ski Resort", "description", "Escape the desert heat and slide down snowy slopes in a giant indoor winter park."),
                Map.of("title", "Jumeirah Beach & Burj Al Arab", "bestTime", "Late Afternoon (4:30 PM - 6:00 PM)", "category", "Scenic Beach", "description", "Relax on soft sand beaches with views of the sail-shaped ultra-luxury hotel."),
                Map.of("title", "Museum of the Future", "bestTime", "Afternoon (2:00 PM - 4:30 PM)", "category", "Museum", "description", "Step into the year 2071 inside an architectural and technological masterpiece."),
                Map.of("title", "Dubai Frame", "bestTime", "Morning (9:00 AM - 11:00 AM)", "category", "Observation Deck", "description", "Stand on the glass walkway bridging old and new Dubai in a giant golden frame.")
            );
        } else if (cleanCity.contains("rome")) {
            return List.of(
                Map.of("title", "Colosseum & Roman Forum", "bestTime", "Morning (8:30 AM - 11:30 AM)", "category", "Ancient Ruins", "description", "Walk the grounds of Rome's ancient gladiatorial arena and political center."),
                Map.of("title", "Vatican Museums & Sistine Chapel", "bestTime", "Morning (9:00 AM - 12:30 PM)", "category", "Art Museum", "description", "Admire legendary art collections, culminating in Michelangelo's ceiling frescoes."),
                Map.of("title", "St. Peter's Basilica", "bestTime", "Midday (11:30 AM - 1:30 PM)", "category", "Historic Basilica", "description", "Gaze up at the massive dome designed by Michelangelo in the heart of Vatican City."),
                Map.of("title", "Trevi Fountain", "bestTime", "Late Night (10:00 PM - 12:00 AM)", "category", "Iconic Fountain", "description", "Toss a coin into the beautiful Baroque masterpiece under night illumination."),
                Map.of("title", "The Pantheon", "bestTime", "Afternoon (2:30 PM - 4:00 PM)", "category", "Ancient Temple", "description", "Marvel at the world's largest unreinforced concrete dome, built 2,000 years ago."),
                Map.of("title", "Spanish Steps", "bestTime", "Sunset (5:30 PM - 7:00 PM)", "category", "Iconic Landmark", "description", "Climb the steps to the Trinità dei Monti church for lovely evening square views."),
                Map.of("title", "Piazza Navona", "bestTime", "Evening (6:00 PM - 8:30 PM)", "category", "Historic Square", "description", "Enjoy Bernini's Fountain of the Four Rivers, street artists, and outdoor dining."),
                Map.of("title", "Castel Sant'Angelo", "bestTime", "Late Afternoon (4:00 PM - 6:00 PM)", "category", "Historic Castle", "description", "Explore the ancient mausoleum of Emperor Hadrian turned fortress and papal castle."),
                Map.of("title", "Villa Borghese Gardens", "bestTime", "Midday (12:00 PM - 2:00 PM)", "category", "Scenic Park", "description", "Walk or cycle through lush park landscapes and visit the famous Galleria Borghese."),
                Map.of("title", "Trastevere Neighborhood", "bestTime", "Night (7:30 PM - 10:30 PM)", "category", "Dining District", "description", "Immerse yourself in authentic Roman nightlife in charming, ivy-draped streets.")
            );
        } else if (cleanCity.contains("singapore")) {
            return List.of(
                Map.of("title", "Gardens by the Bay", "bestTime", "Evening (4:30 PM - 8:30 PM)", "category", "Nature & Gardens", "description", "Marvel at the giant Supertree Grove, Flower Dome, and the Garden Rhapsody light show."),
                Map.of("title", "Marina Bay Sands SkyPark", "bestTime", "Sunset (5:30 PM - 7:30 PM)", "category", "Observation Deck", "description", "Get panoramic views of Singapore's harbour and skyline from the boat-shaped deck."),
                Map.of("title", "Sentosa Island", "bestTime", "Full Day (10:00 AM - 6:00 PM)", "category", "Leisure & Resort", "description", "Enjoy sandy beaches, Cable Car rides, and the Universal Studios theme park."),
                Map.of("title", "Singapore Botanic Gardens", "bestTime", "Early Morning (6:30 AM - 9:00 AM)", "category", "Scenic Park", "description", "Walk through lush landscapes and the National Orchid Garden in a UNESCO site."),
                Map.of("title", "Clarke Quay & Singapore River", "bestTime", "Night (7:30 PM - 10:30 PM)", "category", "Nightlife & Boat", "description", "Take a traditional bumboat cruise and explore vibrant waterside dining and clubs."),
                Map.of("title", "Chinatown & Buddha Tooth Relic Temple", "bestTime", "Morning (9:30 AM - 12:00 PM)", "category", "Culture & Temple", "description", "Admire the grand temple architecture and explore rich Chinese heritage shops."),
                Map.of("title", "Little India & Mustafa Centre", "bestTime", "Afternoon (1:30 PM - 4:30 PM)", "category", "Culture & Shopping", "description", "Experience colorful streets, fragrant spice shops, and 24-hour retail therapy."),
                Map.of("title", "Singapore Zoo & Night Safari", "bestTime", "Night (7:15 PM - 10:30 PM)", "category", "Wildlife", "description", "Ride a tram to observe nocturnal animals in their natural, open-air habitats."),
                Map.of("title", "Merlion Park", "bestTime", "Sunset (6:00 PM - 7:00 PM)", "category", "Iconic Landmark", "description", "Snap photos with Singapore's mythical half-lion, half-fish national icon."),
                Map.of("title", "Orchard Road Shopping Belt", "bestTime", "Midday (11:00 AM - 2:00 PM)", "category", "Shopping", "description", "Walk down miles of connected megamalls housing global fashion and luxury brands.")
            );
        } else if (cleanCity.contains("kyoto")) {
            return List.of(
                Map.of("title", "Fushimi Inari Shrine", "bestTime", "Early Morning (6:00 AM - 8:30 AM)", "category", "Shinto Shrine", "description", "Hike through tunnels of thousands of vibrant vermilion Shinto torii gates."),
                Map.of("title", "Kinkaku-ji (Golden Pavilion)", "bestTime", "Morning (9:00 AM - 11:00 AM)", "category", "Zen Temple", "description", "Marvel at a spectacular Zen Buddhist temple covered in brilliant gold leaf."),
                Map.of("title", "Kiyomizu-dera Temple", "bestTime", "Morning (8:30 AM - 10:30 AM)", "category", "Historic Temple", "description", "Stand on a massive wooden stage for views of Kyoto surrounded by cherry trees."),
                Map.of("title", "Arashiyama Bamboo Grove", "bestTime", "Early Morning (6:30 AM - 8:30 AM)", "category", "Nature", "description", "Walk through towering stalks of green bamboo whispering in the wind."),
                Map.of("title", "Gion Geisha District", "bestTime", "Evening (5:30 PM - 8:00 PM)", "category", "Historic Neighborhood", "description", "Stroll historical wooden streets and catch glimpses of geiko and maiko."),
                Map.of("title", "Ginkaku-ji (Silver Pavilion)", "bestTime", "Afternoon (1:30 PM - 3:30 PM)", "category", "Zen Temple", "description", "Explore beautiful Zen gardens and an understated historic villa."),
                Map.of("title", "Nijo Castle", "bestTime", "Morning (9:30 AM - 11:30 AM)", "category", "Historic Castle", "description", "Walk the 'nightingale floors' that chirp like birds to warn of assassins."),
                Map.of("title", "Philosopher's Path", "bestTime", "Midday (11:00 AM - 1:00 PM)", "category", "Scenic Path", "description", "Walk alongside a canal lined with cherry trees and small local temples."),
                Map.of("title", "Nishiki Market", "bestTime", "Lunchtime (11:30 AM - 2:00 PM)", "category", "Food Market", "description", "Taste local skewers, pickles, matcha snacks, and fresh seafood delicacies."),
                Map.of("title", "Yasaka Shrine & Maruyama Park", "bestTime", "Sunset (5:30 PM - 7:30 PM)", "category", "Shinto Shrine", "description", "Walk through a glowing lantern-lit courtyard in the heart of Gion.")
            );
        }

        // Generic intelligent fallback based on city name to make sure it's customized
        String capitalizedCity = city.substring(0, 1).toUpperCase() + city.substring(1);
        return List.of(
                Map.of("title", capitalizedCity + " Old Town & Historic Quarter", "bestTime", "Morning (9:00 AM - 12:00 PM)", "category", "Historical Landmark", "description", "Explore the cobblestone streets, charming architecture, and local historic monuments."),
                Map.of("title", capitalizedCity + " Botanical Gardens & Park", "bestTime", "Early Morning (6:30 AM - 8:30 AM)", "category", "Nature & Park", "description", "Take a peaceful stroll through lush green landscapes and exotic floral exhibitions."),
                Map.of("title", capitalizedCity + " Museum of Fine Arts", "bestTime", "Afternoon (1:00 PM - 4:00 PM)", "category", "Museum", "description", "Admire stunning galleries showcasing contemporary and historical masterpieces."),
                Map.of("title", capitalizedCity + " Central Market Bazaar", "bestTime", "Morning (10:00 AM - 1:00 PM)", "category", "Shopping & Food", "description", "Sample local street food, fresh produce, and shop for traditional handmade souvenirs."),
                Map.of("title", capitalizedCity + " Skyline Viewpoint & Tower", "bestTime", "Sunset (5:30 PM - 7:00 PM)", "category", "Scenic Outlook", "description", "Capture panoramic views of the entire cityscape as the sun sets over the horizon."),
                Map.of("title", capitalizedCity + " Riverfront Promenade", "bestTime", "Evening (6:00 PM - 9:00 PM)", "category", "Leisure", "description", "Enjoy a breezy evening walk, boat cruises, and vibrant waterside dining."),
                Map.of("title", capitalizedCity + " Cathedral of Saint Mary", "bestTime", "Morning (9:30 AM - 11:30 AM)", "category", "Architecture", "description", "Marvel at the spectacular gothic arches and beautiful stained-glass windows."),
                Map.of("title", capitalizedCity + " Arts & Cultural Centre", "bestTime", "Night (7:30 PM - 10:00 PM)", "category", "Arts & Theatre", "description", "Watch live local theatrical performances, musical shows, and cultural dances."),
                Map.of("title", capitalizedCity + " Street Food Alley", "bestTime", "Night (8:00 PM - 11:00 PM)", "category", "Food & Culinary", "description", "Indulge in mouthwatering local delicacies and late-night snacks from top vendors."),
                Map.of("title", capitalizedCity + " Heritage Library & Archives", "bestTime", "Afternoon (2:00 PM - 5:00 PM)", "category", "Education", "description", "Browse historic manuscripts, old city maps, and enjoy quiet reading halls.")
        );
    }
}
