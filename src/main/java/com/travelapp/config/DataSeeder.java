package com.travelapp.config;

import com.travelapp.entity.Destination;
import com.travelapp.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.travelapp.service.integration.GooglePlacesImageClient;
import com.travelapp.service.integration.WikipediaImageClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DestinationRepository destinationRepository;
    private final GooglePlacesImageClient googlePlacesImageClient;
    private final WikipediaImageClient wikipediaImageClient;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (destinationRepository.count() > 0) {
            log.info("Destinations already seeded, skipping.");
            return;
        }

        log.info("Seeding 15 curated top tourist destinations...");

        List<Destination> destinations = List.of(
                Destination.builder()
                        .name("Paris").country("France").city("Paris")
                        .description("Paris, the City of Light, captivates with its timeless elegance and world-renowned landmarks. The Eiffel Tower, Louvre Museum (home to the Mona Lisa), and Notre-Dame Cathedral define its skyline. Stroll along the Champs-Élysées, explore the artistic haven of Montmartre, and cruise the Seine at sunset. Savor croissants, escargot, coq au vin, and macarons at sidewalk cafés. The Palace of Versailles and Musée d'Orsay are unmissable. Paris Fashion Week and Bastille Day celebrations add cultural vibrancy. The Métro connects everything effortlessly. Best visited April–June and September–October for mild weather and fewer crowds.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/La_Tour_Eiffel_vue_de_la_Tour_Saint-Jacques%2C_Paris_ao%C3%BBt_2014_%282%29.jpg/800px-La_Tour_Eiffel_vue_de_la_Tour_Saint-Jacques%2C_Paris_ao%C3%BBt_2014_%282%29.jpg")
                        .latitude(48.8566).longitude(2.3522)
                        .climate("Temperate oceanic").bestSeason("April–June, Sep–Oct")
                        .tags("romance,culture,food,art,fashion").build(),

                Destination.builder()
                        .name("Tokyo").country("Japan").city("Tokyo")
                        .description("Tokyo seamlessly blends ultramodern skyscrapers with serene ancient temples. Visit Senso-ji Temple in Asakusa, the Imperial Palace gardens, and the vibrant Shibuya Crossing. Akihabara dazzles tech and anime enthusiasts, while Harajuku showcases eccentric street fashion. Enjoy sushi at Tsukiji Outer Market, ramen in Shinjuku, and matcha desserts in Ginza. The cherry blossom season (March–April) transforms parks into pink wonderlands. Mount Fuji is a day trip away. The bullet train (Shinkansen) connects Tokyo to all of Japan. Explore teamLab Borderless and the Meiji Shrine for unforgettable experiences.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/Skyscrapers_of_Shinjuku_2009_January.jpg/800px-Skyscrapers_of_Shinjuku_2009_January.jpg")
                        .latitude(35.6762).longitude(139.6503)
                        .climate("Humid subtropical").bestSeason("Mar–May, Sep–Nov")
                        .tags("technology,culture,food,anime,temples").build(),

                Destination.builder()
                        .name("New York").country("United States").city("New York")
                        .description("New York City, the city that never sleeps, pulses with energy from Times Square to Central Park. The Statue of Liberty, Empire State Building, and Brooklyn Bridge are iconic landmarks. Broadway shows, world-class museums (MoMA, Met), and diverse neighborhoods like SoHo and Harlem offer endless exploration. Sample New York-style pizza, bagels, and cheesecake. The High Line park, 9/11 Memorial, and Fifth Avenue shopping are must-dos. The subway system runs 24/7. Fall (September–November) offers pleasant weather and stunning foliage in Central Park.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/New_york_times_square-terabass.jpg/800px-New_york_times_square-terabass.jpg")
                        .latitude(40.7128).longitude(-74.0060)
                        .climate("Humid subtropical").bestSeason("Sep–Nov, Apr–Jun")
                        .tags("nightlife,culture,shopping,food,iconic").build(),

                Destination.builder()
                        .name("London").country("United Kingdom").city("London")
                        .description("London blends royal grandeur with cutting-edge culture. Buckingham Palace, the Tower of London, and Big Ben are timeless attractions. The British Museum, Tate Modern, and West End theatres offer world-class art and entertainment. Explore Camden Market, stroll Hyde Park, and ride the iconic red double-decker buses. Enjoy fish and chips, afternoon tea, and Sunday roasts. The London Underground (Tube) makes navigation effortless. Visit during spring (April–June) for blooming gardens and pleasant temperatures.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/London_Skyline_%28125508655%29.jpeg/800px-London_Skyline_%28125508655%29.jpeg")
                        .latitude(51.5074).longitude(-0.1278)
                        .climate("Temperate maritime").bestSeason("Apr–Jun, Sep–Oct")
                        .tags("history,culture,theatre,royalty,museums").build(),

                Destination.builder()
                        .name("Dubai").country("United Arab Emirates").city("Dubai")
                        .description("Dubai dazzles with futuristic architecture and luxurious experiences. The Burj Khalifa (world's tallest building), Palm Jumeirah, and Dubai Mall are architectural marvels. Desert safaris, indoor skiing at Ski Dubai, and the Dubai Fountain show offer unique thrills. Savor shawarma, mandi rice, and fine dining at celebrity chef restaurants. The Gold Souk and Spice Souk in Deira offer traditional shopping. The Dubai Metro is modern and efficient. Visit November–March for comfortable outdoor weather away from the extreme summer heat.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Dubai_Marina_Skyline.jpg/800px-Dubai_Marina_Skyline.jpg")
                        .latitude(25.2048).longitude(55.2708)
                        .climate("Hot desert").bestSeason("Nov–Mar")
                        .tags("luxury,shopping,architecture,desert,nightlife").build(),

                Destination.builder()
                        .name("Rome").country("Italy").city("Rome")
                        .description("Rome, the Eternal City, is an open-air museum of ancient civilization. The Colosseum, Roman Forum, and Pantheon transport you to antiquity. Vatican City houses St. Peter's Basilica and Michelangelo's Sistine Chapel ceiling. Toss a coin in the Trevi Fountain, climb the Spanish Steps, and wander Trastevere's cobblestone streets. Indulge in carbonara, cacio e pepe, gelato, and supplì. Rome's bus and metro system connects major sites. Spring (April–June) and early autumn (September–October) offer ideal sightseeing weather.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Colosseum_in_Rome%2C_Italy_-_April_2007.jpg/800px-Colosseum_in_Rome%2C_Italy_-_April_2007.jpg")
                        .latitude(41.9028).longitude(12.4964)
                        .climate("Mediterranean").bestSeason("Apr–Jun, Sep–Oct")
                        .tags("history,culture,food,art,ancient").build(),

                Destination.builder()
                        .name("Bali").country("Indonesia").city("Bali")
                        .description("Bali, the Island of the Gods, enchants with lush rice terraces, volcanic mountains, and pristine beaches. Visit Tanah Lot temple at sunset, explore the Ubud Monkey Forest, and surf at Kuta Beach. Tegallalang Rice Terraces and Mount Batur sunrise trek are unforgettable. Savor nasi goreng, satay, babi guling, and fresh tropical fruits. Balinese dance performances and temple ceremonies showcase rich Hindu culture. Ride scooters or hire private drivers to navigate the island. The dry season (April–October) is ideal for beach holidays and outdoor adventures.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/0/01/Tanah_Lot_Bali.jpg/800px-Tanah_Lot_Bali.jpg")
                        .latitude(-8.3405).longitude(115.0920)
                        .climate("Tropical").bestSeason("Apr–Oct")
                        .tags("beaches,temples,nature,surfing,culture").build(),

                Destination.builder()
                        .name("Sydney").country("Australia").city("Sydney")
                        .description("Sydney captivates with its iconic harbour, world-class beaches, and vibrant culture. The Sydney Opera House and Harbour Bridge are unmistakable landmarks. Bondi Beach, Manly Beach, and the Coastal Walk offer stunning ocean views. The Royal Botanic Garden, Taronga Zoo, and The Rocks historic district are must-visits. Enjoy Australian barramundi, meat pies, pavlova, and flat whites at waterside cafés. Sydney's train and ferry system makes exploration easy. Visit September–November (spring) for warm weather and blooming jacaranda trees.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Sydney_Opera_House_and_Harbour_Bridge_Dusk_%282%29_2019-06-21.jpg/800px-Sydney_Opera_House_and_Harbour_Bridge_Dusk_%282%29_2019-06-21.jpg")
                        .latitude(-33.8688).longitude(151.2093)
                        .climate("Subtropical oceanic").bestSeason("Sep–Nov, Mar–May")
                        .tags("beaches,opera,harbour,wildlife,adventure").build(),

                Destination.builder()
                        .name("Istanbul").country("Turkey").city("Istanbul")
                        .description("Istanbul straddles Europe and Asia, offering a mesmerizing mix of Byzantine and Ottoman heritage. Hagia Sophia, the Blue Mosque, and Topkapi Palace are architectural masterpieces. Cruise the Bosphorus at sunset, haggle at the Grand Bazaar (one of the world's oldest covered markets), and explore the Basilica Cistern. Savor kebabs, baklava, Turkish delight, and strong Turkish coffee. The Istanbulkart smartcard works on metro, tram, and ferries. Spring (April–May) and autumn (September–November) bring mild weather and fewer tourists.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Istanbul_panorama_and_skyline.jpg/800px-Istanbul_panorama_and_skyline.jpg")
                        .latitude(41.0082).longitude(28.9784)
                        .climate("Mediterranean transitional").bestSeason("Apr–May, Sep–Nov")
                        .tags("history,culture,food,bazaars,mosques").build(),

                Destination.builder()
                        .name("Barcelona").country("Spain").city("Barcelona")
                        .description("Barcelona enchants with Gaudí's surreal architecture and Mediterranean coastline. La Sagrada Família, Park Güell, and Casa Batlló are UNESCO-listed masterpieces. Stroll La Rambla, explore the Gothic Quarter, and relax at Barceloneta Beach. Camp Nou is a pilgrimage for football fans. Feast on tapas, paella, churros con chocolate, and Catalan crema. The efficient metro and bus system covers the entire city. Visit May–June or September–October for warm weather without peak-summer crowds and prices.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/NYC_wbread.jpg/800px-NYC_wbread.jpg")
                        .latitude(41.3851).longitude(2.1734)
                        .climate("Mediterranean").bestSeason("May–Jun, Sep–Oct")
                        .tags("architecture,beaches,food,football,nightlife").build(),

                Destination.builder()
                        .name("Santorini").country("Greece").city("Santorini")
                        .description("Santorini is a volcanic island paradise famous for its white-washed buildings, blue-domed churches, and breathtaking sunsets over the Aegean Sea. Oia village offers the most photographed sunset in the world. Explore Fira's cliffside paths, visit the ancient ruins of Akrotiri, and swim at the unique Red Beach and Black Sand Beach. Enjoy moussaka, souvlaki, fresh seafood, and local Vinsanto wine. Ferry and bus services connect the main towns. Late April–October is peak season, but May–June offers the best balance of weather and crowd size.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/20100726_Oia_Santorini.jpg/800px-20100726_Oia_Santorini.jpg")
                        .latitude(36.3932).longitude(25.4615)
                        .climate("Mediterranean").bestSeason("May–Jun, Sep–Oct")
                        .tags("islands,sunsets,romance,beaches,photography").build(),

                Destination.builder()
                        .name("Cape Town").country("South Africa").city("Cape Town")
                        .description("Cape Town sits beneath the dramatic flat-topped Table Mountain, where a cable car ride reveals panoramic views of the Atlantic coastline. Visit Robben Island (Nelson Mandela's prison), explore the colorful Bo-Kaap neighborhood, and drive the stunning Chapman's Peak Drive. The V&A Waterfront combines shopping, dining, and ocean views. Enjoy bobotie, biltong, braai, and world-class wines from nearby Stellenbosch and Franschhoek. Cape Point and the penguin colony at Boulders Beach are unforgettable. Visit October–March for warm, sunny weather.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Table_Mountain_DanieVDM.jpg/800px-Table_Mountain_DanieVDM.jpg")
                        .latitude(-33.9249).longitude(18.4241)
                        .climate("Mediterranean").bestSeason("Oct–Mar")
                        .tags("mountains,wildlife,wine,beaches,culture").build(),

                Destination.builder()
                        .name("Singapore").country("Singapore").city("Singapore")
                        .description("Singapore is a gleaming city-state blending futuristic architecture with lush tropical gardens. Marina Bay Sands, Gardens by the Bay (with the iconic Supertree Grove), and Merlion Park define the skyline. Visit Sentosa Island for beaches and Universal Studios, explore Chinatown and Little India for cultural immersion, and shop on Orchard Road. Hawker centres serve legendary chilli crab, Hainanese chicken rice, laksa, and char kway teow at affordable prices. The MRT system is world-class. Singapore is a year-round destination with consistent tropical weather.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Marina_Bay_Sands_in_the_evening_-_20101120.jpg/800px-Marina_Bay_Sands_in_the_evening_-_20101120.jpg")
                        .latitude(1.3521).longitude(103.8198)
                        .climate("Tropical rainforest").bestSeason("Year-round")
                        .tags("food,shopping,gardens,modern,clean").build(),

                Destination.builder()
                        .name("Maldives").country("Maldives").city("Malé")
                        .description("The Maldives is a tropical paradise of 1,192 coral islands scattered across the Indian Ocean, renowned for overwater villas, crystal-clear turquoise lagoons, and vibrant coral reefs. Snorkel with manta rays and whale sharks, dive into some of the world's best underwater sites, and relax on powder-white beaches. Malé's Hukuru Miskiiy (Old Friday Mosque) and fish market offer cultural glimpses. Enjoy Maldivian mas huni (tuna and coconut), garudhiya (fish soup), and fresh lobster. Speedboat and seaplane transfers connect resort islands. November–April is the dry season with the best visibility for diving.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Maldives_small_island.jpg/800px-Maldives_small_island.jpg")
                        .latitude(3.2028).longitude(73.2207)
                        .climate("Tropical monsoon").bestSeason("Nov–Apr")
                        .tags("beaches,luxury,diving,romance,islands").build(),

                Destination.builder()
                        .name("Kyoto").country("Japan").city("Kyoto")
                        .description("Kyoto, Japan's ancient imperial capital, preserves over 2,000 temples, shrines, and traditional wooden machiya townhouses. Fushimi Inari Shrine's 10,000 vermilion torii gates, Kinkaku-ji (Golden Pavilion), and Arashiyama Bamboo Grove are iconic. Experience a traditional tea ceremony in a Gion teahouse and spot geisha in the historic district. Savor kaiseki (multi-course haute cuisine), matcha sweets, yudofu (tofu hot pot), and Nishiki Market street food. The city bus system and bicycle rentals make exploration easy. Cherry blossom season (March–April) and autumn foliage (November) are peak beauty periods.")
                        .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/Kinkaku-ji_the_Golden_Temple_in_Kyoto_overlooking_the_lake_-_high_res.JPG/800px-Kinkaku-ji_the_Golden_Temple_in_Kyoto_overlooking_the_lake_-_high_res.JPG")
                        .latitude(35.0116).longitude(135.7681)
                        .climate("Humid subtropical").bestSeason("Mar–May, Oct–Nov")
                        .tags("temples,culture,food,tradition,nature").build()
        );

        for (Destination dest : destinations) {
            String imgUrl = googlePlacesImageClient.fetchImageForQuery(dest.getCity());
            if (imgUrl == null || imgUrl.isBlank()) {
                imgUrl = wikipediaImageClient.fetchImageForQuery(dest.getCity());
            }
            if (imgUrl != null && !imgUrl.isBlank()) {
                dest.setImageUrl(imgUrl);
            }
        }

        destinationRepository.saveAll(destinations);
        log.info("Seeded {} curated tourist destinations.", destinations.size());
    }
}
