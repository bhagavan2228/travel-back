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
            entry("Hyderabad", "Hyderabad", "Telangana", "India",
                    "Hyderabad, the City of Pearls, is the capital of Telangana and one of India's most vibrant metropolises. " +
                    "Founded in 1591 by Muhammad Quli Qutb Shah, it is home to the iconic Charminar, a stunning 16th-century mosque and monument that serves as the city's emblem. " +
                    "The Golconda Fort, once a mighty diamond trading center, offers breathtaking views and a famous sound-and-light show. " +
                    "Hyderabad is world-renowned for its Hyderabadi Biryani, a fragrant rice dish prepared with aromatic spices, saffron, and slow-cooked meat. " +
                    "Other culinary treasures include Haleem (a slow-cooked stew popular during Ramadan), Irani Chai, Osmania biscuits, and double ka meetha. " +
                    "The Hussain Sagar Lake, with its monolithic Buddha statue, is a popular recreational spot. " +
                    "Ramoji Film City, the world's largest film studio complex, is a must-visit attraction. " +
                    "The Salar Jung Museum houses one of the largest one-man collections of antiques in the world. " +
                    "HITEC City and Gachibowli form a thriving IT corridor, earning Hyderabad the nickname 'Cyberabad'. " +
                    "The Chowmahalla Palace, the seat of the Nizams, showcases exquisite royal architecture and vintage car collections. " +
                    "Laad Bazaar near Charminar is famous for lac bangles and traditional shopping. " +
                    "The Birla Mandir, built entirely of white Rajasthani marble, offers panoramic city views. " +
                    "Hyderabad's climate is semi-arid with hot summers and pleasant winters; the best time to visit is October to March. " +
                    "The city has an excellent public transport network including the Hyderabad Metro. " +
                    "Festivals like Bonalu, Bathukamma, and the grand Eid celebrations reflect the city's rich cultural tapestry.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/7/71/Charminar_Hyderabad_1702.jpg/800px-Charminar_Hyderabad_1702.jpg", 17.3850, 78.4867,
                    "hyderabad,telangana,biryani,charminar,india"),
            entry("Visakhapatnam", "Visakhapatnam", "Andhra Pradesh", "India",
                    "Visakhapatnam (Vizag), the Jewel of the East Coast, is a stunning coastal city in Andhra Pradesh known for its golden beaches, lush green hills, and vibrant port life. " +
                    "Ramakrishna Beach (RK Beach) is the city's most popular stretch, lined with a scenic promenade, the INS Kursura Submarine Museum, and the Dolphins Nose lighthouse. " +
                    "The Araku Valley, a picturesque hill station 3 hours away, is famous for its coffee plantations, tribal culture, and the magnificent Borra Caves with million-year-old stalactites. " +
                    "Kailasagiri Hill offers panoramic views of the Bay of Bengal with a massive Shiva-Parvati sculpture and a ropeway ride. " +
                    "The city's seafood is legendary — try the Vizag-style fish curry, prawn biryani, and bamboo chicken. " +
                    "Simhachalam Temple, an 11th-century shrine dedicated to Lord Narasimha, is one of the most visited temples in South India. " +
                    "The Indira Gandhi Zoological Park is one of the largest zoos in India, spread across 625 acres. " +
                    "Yarada Beach, secluded and pristine, is considered one of the most beautiful beaches in India. " +
                    "Visakhapatnam port is the oldest operating port in India and one of the busiest. " +
                    "The submarine museum (INS Kursura S20) is the first of its kind in South Asia. " +
                    "Ross Hill Church, one of the oldest churches in the region, sits atop a scenic hill. " +
                    "Vizag's climate is tropical with warm temperatures year-round; the best time to visit is October to March when the weather is pleasant. " +
                    "The Vizag Steel Plant is Asia's largest shore-based steel plant. " +
                    "Rushikonda Beach is perfect for water sports including surfing, jet skiing, and parasailing. " +
                    "The city is emerging as a major IT hub with the development of the Fintech Valley.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/View_from_Kailasagiri_park.jpg/800px-View_from_Kailasagiri_park.jpg", 17.6868, 83.2185,
                    "vizag,visakhapatnam,andhra pradesh,beach,india"),
            entry("Vijayawada", "Vijayawada", "Andhra Pradesh", "India",
                    "Vijayawada, the City of Victory, is the commercial and cultural heart of Andhra Pradesh, situated on the banks of the sacred Krishna River. " +
                    "The Kanaka Durga Temple atop Indrakeeladri Hill is one of the most revered shrines in South India, drawing millions of devotees annually. " +
                    "Prakasam Barrage, a 1,223-meter dam across the Krishna River, is an engineering marvel and a scenic sunset spot. " +
                    "Undavalli Caves, dating back to the 4th-5th century, feature stunning rock-cut architecture with a monolithic reclining Vishnu statue. " +
                    "Bhavani Island, one of the largest river islands in India, is a popular picnic and adventure destination. " +
                    "The city is a gateway to Amaravathi, the ancient Buddhist capital where the famous Amaravathi Stupa once stood. " +
                    "Vijayawada's cuisine is known for authentic Andhra flavors — fiery gunpowder (podi), tangy gongura pachadi, and the famous Atla Taddi celebrations. " +
                    "Rajiv Gandhi Park and the Gandhi Hill Museum offer recreational and educational experiences. " +
                    "The city has excellent connectivity as a major railway junction. " +
                    "Victoria Museum showcases local history and archaeological artifacts. " +
                    "Kondapalli Fort and its famous handcrafted Kondapalli toys are significant cultural landmarks. " +
                    "Vijayawada experiences a tropical climate with extremely hot summers; the best visiting season is November to February. " +
                    "The Dasara Navaratri celebrations at Kanaka Durga Temple are among the grandest in India. " +
                    "Mogalrajapuram Caves contain rare Ardhanarishvara sculptures dating to the 5th century. " +
                    "The city is rapidly developing as part of the Amaravati Capital Region plan.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f8/Prakasam_Barrage_vijayawada.jpg/800px-Prakasam_Barrage_vijayawada.jpg", 16.5062, 80.6480,
                    "vijayawada,andhra pradesh,india"),
            entry("Bangalore", "Bengaluru", "Karnataka", "India",
                    "Bengaluru (Bangalore), the Silicon Valley of India, is Karnataka's capital and the country's premier technology and startup hub. " +
                    "Known as the Garden City for its lush parks and pleasant climate, Bengaluru sits at an elevation of 920 meters, giving it year-round mild weather. " +
                    "Lalbagh Botanical Garden, established in 1760 by Hyder Ali, spans 240 acres and houses rare tropical plants and a famous glass house modeled on London's Crystal Palace. " +
                    "Cubbon Park, a 300-acre green lung in the city center, is home to the State Library, High Court, and museums. " +
                    "The Bangalore Palace, inspired by England's Windsor Castle, features Tudor-style architecture with beautiful woodcarvings. " +
                    "Bengaluru's culinary scene is extraordinary — from classic Benne Dosa at CTR and Vidyarthi Bhavan to the thriving microbrewery culture with over 100 craft beer venues. " +
                    "Commercial Street and Brigade Road are iconic shopping and entertainment hubs. " +
                    "ISKCON Temple Bangalore is one of the largest ISKCON temples in the world. " +
                    "Nandi Hills, just 60 km away, is a popular sunrise viewpoint and weekend getaway. " +
                    "The city has a vibrant nightlife scene and is India's pub capital. " +
                    "Tipu Sultan's Summer Palace, an elegant Indo-Islamic wooden structure, is a historic gem. " +
                    "HAL Aerospace Museum showcases India's aviation heritage with vintage aircraft. " +
                    "Bannerghatta National Park on the city outskirts offers a butterfly park, zoo, and safari experience. " +
                    "UB City is Bengaluru's luxury lifestyle destination with high-end shopping and dining. " +
                    "The best time to visit is September to February when the weather is most pleasant.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Bangalore_Vidhan_Soudha.jpg/800px-Bangalore_Vidhan_Soudha.jpg", 12.9716, 77.5946,
                    "bangalore,bengaluru,karnataka,india"),
            entry("Mumbai", "Mumbai", "Maharashtra", "India",
                    "Mumbai, the Maximum City, is India's financial capital and the home of Bollywood, pulsating with an energy that never sleeps. " +
                    "The Gateway of India, built in 1924 to commemorate King George V's visit, is the city's most iconic landmark overlooking the Arabian Sea. " +
                    "Marine Drive, known as the Queen's Necklace for its sparkling arc of streetlights at night, is a 3.6-km promenade perfect for sunset walks. " +
                    "The Chhatrapati Shivaji Maharaj Terminus (CST), a UNESCO World Heritage Site, is a stunning example of Victorian Gothic Revival architecture. " +
                    "Mumbai's street food is legendary — Vada Pav (the Mumbai burger), Pav Bhaji, Bhel Puri at Chowpatty Beach, and Bombay Sandwich are unmissable. " +
                    "The Elephanta Caves, a UNESCO site on an island in the harbor, feature magnificent 5th-century rock-cut sculptures of Lord Shiva. " +
                    "Dharavi, one of Asia's largest slums, is also a $1 billion informal economy and a testament to entrepreneurial spirit. " +
                    "The Bandra-Worli Sea Link, a cable-stayed bridge, is an engineering marvel connecting South and West Mumbai. " +
                    "Haji Ali Dargah, a stunning mosque built on an islet, is accessible only at low tide. " +
                    "Sanjay Gandhi National Park, spread over 104 sq km, is one of the few national parks within a city, home to the ancient Kanheri Caves. " +
                    "Colaba Causeway and Linking Road are popular markets for street shopping. " +
                    "The local train network carries over 7.5 million commuters daily and is the city's lifeline. " +
                    "Juhu Beach and Bandstand are celebrity neighborhoods and popular hangout spots. " +
                    "Mumbai experiences a tropical monsoon climate; the best time to visit is November to February. " +
                    "The city's dabbawalas, who deliver 200,000 lunch boxes daily with near-zero errors, are studied by Harvard Business School.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Mumbai_03-2016_30_Gateway_of_India.jpg/800px-Mumbai_03-2016_30_Gateway_of_India.jpg", 19.0760, 72.8777,
                    "mumbai,bombay,maharashtra,india"),
            entry("Delhi", "New Delhi", "Delhi", "India",
                    "Delhi, India's sprawling capital territory, is a mesmerizing blend of ancient heritage and modern ambition spanning over 3,000 years of history. " +
                    "The Red Fort (Lal Qila), a UNESCO World Heritage Site built by Mughal emperor Shah Jahan in 1639, is where India's Independence Day flag hoisting takes place annually. " +
                    "Qutub Minar, the tallest brick minaret in the world at 72.5 meters, dates back to the 12th century and is surrounded by stunning Indo-Islamic architecture. " +
                    "Humayun's Tomb, the first garden-tomb in the Indian subcontinent, inspired the design of the Taj Mahal. " +
                    "India Gate, a 42-meter war memorial, is the heart of Lutyens' Delhi and a beloved gathering spot. " +
                    "Chandni Chowk, one of the oldest and busiest markets in India, is a paradise for street food lovers — parantha at Paranthe Wali Gali, jalebi at Old Famous, and chaat at Natraj. " +
                    "The Lotus Temple, shaped like a lotus flower, is a Bahá'í House of Worship open to all faiths. " +
                    "Akshardham Temple is a stunning modern Hindu temple complex featuring intricate stone carvings and boat rides. " +
                    "Jama Masjid, built by Shah Jahan, is one of the largest mosques in India with capacity for 25,000 worshippers. " +
                    "The Delhi Metro is one of the most efficient urban transit systems in the world, covering over 390 km. " +
                    "Hauz Khas Village combines medieval ruins with trendy cafes, art galleries, and boutiques. " +
                    "Delhi's climate has extreme seasons — scorching summers (up to 47°C) and chilly winters; the best time to visit is October to March. " +
                    "Lodhi Garden, a beautiful park with 15th-century tombs, is a peaceful escape in the heart of the city. " +
                    "Connaught Place (CP) is the commercial and cultural hub with its distinctive Georgian architecture. " +
                    "The National Museum, Crafts Museum, and Rail Museum offer deep cultural immersion.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Qutab_Minar_mbread.jpg/800px-Qutab_Minar_mread.jpg", 28.6139, 77.2090,
                    "delhi,new delhi,india"),
            entry("Chennai", "Chennai", "Tamil Nadu", "India",
                    "Chennai, the Gateway to South India, is the capital of Tamil Nadu and a vibrant hub of Dravidian culture, classical arts, and beach life. " +
                    "Marina Beach, stretching 13 km, is the second longest urban beach in the world and a beloved gathering spot for locals. " +
                    "Kapaleeshwarar Temple, a 7th-century Dravidian-style masterpiece in Mylapore, is one of the most important Shiva temples in South India. " +
                    "Fort St. George, built in 1644 by the British East India Company, is the first English fortress in India and now houses the Tamil Nadu Legislative Assembly. " +
                    "Chennai's filter coffee is legendary — brewed fresh with chicory, it's served in traditional steel tumblers and dabara sets. " +
                    "The city's cuisine includes crispy dosas at Murugan Idli Shop, fluffy idlis with sambar, spicy Chettinad chicken, and kothu parotta. " +
                    "San Thome Basilica, built over the tomb of St. Thomas the Apostle, is one of only three churches in the world built over an apostle's tomb. " +
                    "The Government Museum in Egmore has an impressive collection of South Indian bronzes and archaeological artifacts. " +
                    "Mahabalipuram (1 hour drive), a UNESCO World Heritage Site, features stunning 7th-century rock-cut temples and the famous Shore Temple. " +
                    "Chennai is the automobile capital of India and the Detroit of Asia. " +
                    "The Music Season (December-January), a six-week festival of Carnatic music and Bharatanatyam dance, is the world's largest cultural event. " +
                    "Elliot's Beach (Besant Nagar) is a quieter, cleaner alternative to Marina Beach. " +
                    "T. Nagar and Pondy Bazaar are the city's premium shopping destinations for silk sarees and gold jewelry. " +
                    "Chennai has a hot and humid tropical climate; the best time to visit is November to February. " +
                    "The city is home to major film studios producing Tamil cinema (Kollywood), the second largest film industry in India.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ef/Chennai_Central.jpg/800px-Chennai_Central.jpg", 13.0827, 80.2707,
                    "chennai,madras,tamil nadu,india"),
            entry("Kolkata", "Kolkata", "West Bengal", "India",
                    "Kolkata, the City of Joy, is the cultural capital of India and the former capital of British India, brimming with literary heritage, art, and soul. " +
                    "The Victoria Memorial, an exquisite white marble monument built between 1906-1921, is Kolkata's crown jewel and houses a museum of British-Indian history. " +
                    "Howrah Bridge (Rabindra Setu), one of the busiest cantilever bridges in the world, spans the Hooghly River and is an engineering icon. " +
                    "The Durga Puja festival in Kolkata is a UNESCO Intangible Cultural Heritage, transforming the city into an open-air art gallery with thousands of elaborate pandals. " +
                    "Kolkata's street food is unmatched — Kathi Rolls at Nizam's, phuchka (puri filled with tamarind water), Kolkata-style biryani with potato, and mishti doi (sweet yogurt). " +
                    "The Indian Museum, established in 1814, is the oldest and largest multi-purpose museum in Asia. " +
                    "Park Street, known as the 'Food Street', is lined with iconic restaurants like Peter Cat (for Chelo Kebab) and Mocambo. " +
                    "College Street, with its sprawling book market, is the largest second-hand book market in the world. " +
                    "Mother Teresa's Missionaries of Charity headquarters is located in the city, and her tomb is a major pilgrimage site. " +
                    "The Kolkata tram system is the oldest operating electric tram network in Asia. " +
                    "Dakshineswar Kali Temple and Belur Math, associated with Sri Ramakrishna and Swami Vivekananda, are major spiritual centers. " +
                    "Kumartuli, the potters' quarter, is where artisans sculpt thousands of Durga idols each year. " +
                    "The Eden Gardens cricket stadium is one of the most famous cricket venues in the world. " +
                    "Kolkata has a humid subtropical climate with hot summers and mild winters; the best time to visit is October to March. " +
                    "The Yellow Taxi and hand-pulled rickshaws are iconic symbols of the city's old-world charm.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Victoria_Memorial%2C_Kolkata.jpg/800px-Victoria_Memorial%2C_Kolkata.jpg", 22.5726, 88.3639,
                    "kolkata,calcutta,west bengal,india"),
            entry("Jaipur", "Jaipur", "Rajasthan", "India",
                    "Jaipur, the Pink City, is the capital of Rajasthan and a UNESCO World Heritage City, famous for its magnificent forts, palaces, and vibrant culture. " +
                    "Hawa Mahal (Palace of Winds), with its 953 tiny windows, is an iconic five-story pyramidal monument built in 1799 for royal women to observe street life. " +
                    "Amber Fort (Amer Fort), a majestic hilltop fortress blending Hindu and Mughal architecture, features the stunning Sheesh Mahal (Hall of Mirrors). " +
                    "The City Palace complex, still partially occupied by the royal family, showcases a beautiful fusion of Rajasthani and Mughal architecture. " +
                    "Jantar Mantar, a UNESCO World Heritage astronomical observation site, features the world's largest stone sundial. " +
                    "Nahargarh Fort offers spectacular sunset views of the city and the surrounding Aravalli Hills. " +
                    "Jaipur's cuisine is a royal feast — Dal Baati Churma, Laal Maas (fiery red meat curry), Ghewar (festive sweet), and Pyaaz Kachori are must-try dishes. " +
                    "Johari Bazaar and Bapu Bazaar are famous for traditional Rajasthani jewelry, block-printed textiles, and handicrafts. " +
                    "The Albert Hall Museum, the oldest museum in Rajasthan, is housed in a stunning Indo-Saracenic building. " +
                    "Jal Mahal (Water Palace), floating in the middle of Man Sagar Lake, creates a surreal photo opportunity. " +
                    "The Birla Planetarium and Birla Temple (Lakshmi Narayan Temple) are popular modern attractions. " +
                    "Jaipur is part of India's famous Golden Triangle tourist circuit along with Delhi and Agra. " +
                    "The Elephant Festival and Jaipur Literature Festival are major annual events. " +
                    "Jaipur has a semi-arid climate with very hot summers; the best time to visit is October to March. " +
                    "Chokhi Dhani is an ethnic village resort offering an authentic Rajasthani cultural experience with folk dances, puppet shows, and traditional cuisine.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Hawa_Mahal_2011.jpg/800px-Hawa_Mahal_2011.jpg", 26.9124, 75.7873,
                    "jaipur,rajasthan,india"),
            entry("Goa", "Panaji", "Goa", "India",
                    "Goa, India's smallest state by area, is the country's premier beach destination and a melting pot of Indian and Portuguese cultures. " +
                    "Baga Beach and Calangute Beach are the most popular tourist beaches, known for their vibrant nightlife, shacks, and water sports. " +
                    "Palolem Beach in South Goa is a crescent-shaped paradise ideal for kayaking, dolphin watching, and silent noise parties. " +
                    "The Basilica of Bom Jesus, a UNESCO World Heritage Site, houses the mortal remains of St. Francis Xavier and showcases stunning Baroque architecture. " +
                    "Se Cathedral, one of the largest churches in Asia, is an outstanding example of Portuguese Manueline architecture. " +
                    "Goa's cuisine is a unique Indo-Portuguese fusion — Fish Curry Rice is the staple, complemented by Bebinca (layered dessert), Vindaloo, Xacuti, and Prawn Balchão. " +
                    "Feni, a locally distilled spirit made from cashew or coconut, is Goa's signature drink. " +
                    "Dudhsagar Falls, a magnificent 310-meter waterfall on the Goa-Karnataka border, is a spectacular monsoon attraction. " +
                    "Fort Aguada, built in 1612 by the Portuguese, offers panoramic views of the Arabian Sea. " +
                    "Spice plantations in Ponda offer guided tours with traditional Goan lunches. " +
                    "Anjuna Flea Market and Saturday Night Market at Arpora are iconic shopping experiences. " +
                    "Old Goa (Velha Goa) was once called the 'Rome of the East' and features magnificent colonial churches. " +
                    "Goa's carnival, celebrated in February, is a colorful three-day festival with parades, music, and dance. " +
                    "Goa has a tropical monsoon climate; the best time to visit is November to February for beaches and June to September for waterfall treks. " +
                    "The Goan trance music scene and beach parties at Vagator and Chapora have made Goa world-famous in electronic music culture.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Palolem_Beach.jpg/800px-Palolem_Beach.jpg", 15.4909, 73.8278,
                    "goa,panaji,beach,india"),
            entry("Pune", "Pune", "Maharashtra", "India",
                    "Pune, the Oxford of the East, is Maharashtra's cultural capital and a thriving city of education, IT, and automotive industries. " +
                    "Shaniwar Wada, the 18th-century fortified palace of the Peshwa rulers, is Pune's most iconic historical landmark with a famous nightly light-and-sound show. " +
                    "Aga Khan Palace, where Mahatma Gandhi and Kasturba Gandhi were imprisoned during the Quit India Movement, is now a memorial. " +
                    "Sinhagad Fort, perched 1,300 meters above sea level, is associated with the legendary battle of Tanaji Malusare and offers thrilling trekking trails. " +
                    "The Osho International Meditation Resort in Koregaon Park attracts seekers from around the world. " +
                    "Pune's food culture is extraordinary — Misal Pav at Bedekar or Katakirr, Vada Pav, Bhakarwadi, and Mastani (thick milkshake) at Sujata Mastani. " +
                    "FC Road and JM Road are iconic college-town hangouts with street food stalls and bookshops. " +
                    "The Raja Dinkar Kelkar Museum has over 20,000 artifacts showcasing Indian heritage. " +
                    "Lavasa, an Italian-themed planned city near Pune, is a popular weekend getaway. " +
                    "Pune's IT parks in Hinjewadi and Kharadi are major technology hubs. " +
                    "The city hosts the Pune International Film Festival and Sawai Gandharva Music Festival annually. " +
                    "Dagdusheth Halwai Ganpati Temple is one of the richest and most famous Ganesh temples in India. " +
                    "Parvati Hill Temple offers panoramic views of the city and houses a museum with Peshwa-era artifacts. " +
                    "Pune has a pleasant semi-arid climate with mild winters; the best time to visit is October to March. " +
                    "The city is the headquarters of the Indian Army's Southern Command and home to the National Defence Academy (NDA).",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b5/Shaniwarwada_Pune_India.jpg/800px-Shaniwarwada_Pune_India.jpg", 18.5204, 73.8567,
                    "pune,maharashtra,india"),
            entry("Kochi", "Kochi", "Kerala", "India",
                    "Kochi (Cochin), the Queen of the Arabian Sea, is Kerala's vibrant port city and a fascinating blend of European colonial history, Jewish heritage, and South Indian culture. " +
                    "The iconic Chinese Fishing Nets at Fort Kochi, introduced by Chinese traders in the 14th century, are a unique fishing technique found nowhere else in India. " +
                    "Fort Kochi's heritage district features charming colonial streets, art galleries, cafes, and the historic Mattancherry Palace (Dutch Palace) with its stunning Hindu murals. " +
                    "The Paradesi Synagogue in Jew Town, built in 1568, is the oldest active synagogue in the Commonwealth. " +
                    "St. Francis Church, built in 1503, is the oldest European church in India — Vasco da Gama was originally buried here. " +
                    "Kochi's seafood is legendary — Karimeen (Pearl Spot fish) fry, Kerala-style prawn curry in coconut milk, and Malabar Biryani are must-try dishes. " +
                    "The Kochi-Muziris Biennale is India's largest contemporary art exhibition, held every two years. " +
                    "Alleppey (Alappuzha) backwaters, just 1.5 hours from Kochi, offer unforgettable houseboat cruises through Kerala's famous waterways. " +
                    "Marine Drive, Kochi's beautiful waterfront promenade, is perfect for evening walks. " +
                    "Cherai Beach, a 15-km stretch of golden sand, is ideal for swimming and watching dolphins. " +
                    "Hill Palace Museum in Tripunithura is the largest archaeological museum in Kerala. " +
                    "The Kathakali Centre at Fort Kochi offers nightly performances of Kerala's classical dance drama. " +
                    "Lulu Mall in Kochi is one of the largest shopping malls in India. " +
                    "Kochi has a tropical monsoon climate; the best time to visit is September to March. " +
                    "The city's spice trade history dates back over 600 years, making it a key player in the global spice route.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1f/Chinese_Fishing_Nets_Kochi.jpg/800px-Chinese_Fishing_Nets_Kochi.jpg", 9.9312, 76.2673,
                    "kochi,kerala,india"),
            entry("Paris", "Paris", "Île-de-France", "France",
                    "Paris, the City of Light, is the capital of France and one of the most visited cities in the world, celebrated for its art, fashion, gastronomy, and romantic ambiance. " +
                    "The Eiffel Tower, built by Gustave Eiffel for the 1889 World Fair, stands 330 meters tall and offers breathtaking panoramic views from three observation levels. " +
                    "The Louvre Museum, housed in a former royal palace, is the world's largest and most visited art museum, home to the Mona Lisa and Venus de Milo. " +
                    "Notre-Dame Cathedral, a masterpiece of French Gothic architecture dating to 1163, is undergoing restoration after the devastating 2019 fire. " +
                    "The Champs-Élysées, stretching from the Arc de Triomphe to Place de la Concorde, is one of the most famous avenues in the world. " +
                    "Montmartre, crowned by the white-domed Sacré-Cœur Basilica, is the artistic heart of Paris where Picasso, Van Gogh, and Monet once lived and worked. " +
                    "French cuisine is a UNESCO Intangible Cultural Heritage — try croissants, escargot, coq au vin, crêpes, macarons from Ladurée, and authentic Parisian bistro meals. " +
                    "The Musée d'Orsay, housed in a stunning Beaux-Arts railway station, has the world's finest collection of Impressionist and Post-Impressionist masterpieces. " +
                    "The Palace of Versailles, 30 minutes from central Paris, is the epitome of French royal grandeur with its Hall of Mirrors and magnificent gardens. " +
                    "The Latin Quarter and Saint-Germain-des-Prés are charming neighborhoods with historic bookshops, jazz clubs, and intellectual cafes. " +
                    "The Seine River cruises (Bateaux Mouches) offer magical nighttime views of illuminated monuments. " +
                    "Le Marais is a trendy district known for its Jewish Quarter, art galleries, vintage boutiques, and the Place des Vosges. " +
                    "Paris Fashion Week is one of the Big Four fashion events, showcasing major global designers. " +
                    "Paris has a temperate oceanic climate with mild summers and cold winters; the best time to visit is April to June and September to November. " +
                    "The Paris Métro, with 300+ stations, is one of the most efficient and comprehensive urban rail networks in the world.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/Tour_Eiffel_Wikimedia_Commons.jpg/800px-Tour_Eiffel_Wikimedia_Commons.jpg", 48.8566, 2.3522,
                    "paris,france,europe"),
            entry("Tokyo", "Tokyo", "Kanto", "Japan",
                    "Tokyo, Japan's electrifying capital, is a city where ultramodern skyscrapers stand alongside ancient temples, creating a captivating urban tapestry. " +
                    "Shibuya Crossing, the world's busiest pedestrian intersection, is an iconic symbol of Tokyo's organized chaos with up to 3,000 people crossing at once. " +
                    "Senso-ji Temple in Asakusa, Tokyo's oldest temple founded in 645 AD, features the magnificent Kaminarimon (Thunder Gate) and bustling Nakamise shopping street. " +
                    "The Tokyo Skytree, at 634 meters, is the world's tallest tower and offers 360-degree views of the vast metropolitan area. " +
                    "Meiji Shrine, nestled in 170 acres of forest in the heart of Harajuku, is a serene Shinto shrine dedicated to Emperor Meiji and Empress Shoken. " +
                    "Tokyo's food scene is unrivaled — it has the most Michelin-starred restaurants of any city in the world. From Tsukiji Outer Market sushi to ramen in Shinjuku, every meal is extraordinary. " +
                    "Akihabara (Electric Town) is the global epicenter of anime, manga, electronics, and otaku culture. " +
                    "Shinjuku, with its neon-lit skyscrapers, hosts the busiest train station in the world (3.5 million daily passengers) and the atmospheric Golden Gai bar district. " +
                    "The Imperial Palace, surrounded by stone walls and moats, is the primary residence of Japan's Emperor. " +
                    "Harajuku's Takeshita Street is the heart of Japanese youth fashion and pop culture. " +
                    "Roppongi Hills and Odaiba are modern entertainment districts with museums, shopping, and dining. " +
                    "TeamLab Borderless and Planets are immersive digital art museums that redefine the art experience. " +
                    "Cherry blossom season (hanami) in March-April transforms parks like Ueno and Shinjuku Gyoen into magical pink wonderlands. " +
                    "Tokyo has a humid subtropical climate with hot summers and mild winters; the best time to visit is March to May and September to November. " +
                    "The city's rail and subway network is legendary for its precision — trains run to the second.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/Skyscrapers_of_Shinjuku_2009_January.jpg/800px-Skyscrapers_of_Shinjuku_2009_January.jpg", 35.6762, 139.6503,
                    "tokyo,japan,asia"),
            entry("Switzerland", "Zurich", "Zurich", "Switzerland",
                    "Switzerland, the heart of Europe, is a breathtaking Alpine nation renowned for its stunning mountain landscapes, pristine lakes, world-class chocolates, and precision watchmaking. " +
                    "The Matterhorn, one of the highest peaks in the Alps at 4,478 meters, is one of the most photographed mountains in the world and is accessible from the charming village of Zermatt. " +
                    "Zurich, Switzerland's largest city, is a global banking center with a beautifully preserved medieval old town (Altstadt) along the Limmat River. " +
                    "Lake Geneva, shared with France, is Europe's largest Alpine lake and is surrounded by vineyards, castles, and the headquarters of international organizations. " +
                    "Interlaken, situated between Lake Thun and Lake Brienz, is the adventure capital offering paragliding, skydiving, and a gateway to the Jungfraujoch — the Top of Europe at 3,454 meters. " +
                    "Swiss chocolate is world-famous — visit the Lindt Chocolate Museum in Zurich or Maison Cailler in Broc for tastings and history. " +
                    "Swiss cuisine includes fondue (melted cheese with bread), raclette, rösti (crispy potato cake), and Zürcher Geschnetzeltes (veal in cream sauce). " +
                    "The Glacier Express, running from Zermatt to St. Moritz, is one of the world's most scenic train journeys, crossing 291 bridges and 91 tunnels. " +
                    "Lucerne, with its iconic Chapel Bridge (Kapellbrücke) and Lion Monument, is one of Switzerland's most picturesque cities. " +
                    "The Swiss Alps offer world-class skiing in resorts like Verbier, St. Moritz, Davos, and Grindelwald. " +
                    "Bern, the capital, features a UNESCO-listed medieval old town with the famous Zytglogge clock tower. " +
                    "CERN, the European Organization for Nuclear Research, is located near Geneva and offers public tours. " +
                    "Swiss watches from brands like Rolex, Patek Philippe, and Omega represent the pinnacle of horology. " +
                    "Switzerland has a temperate climate that varies with altitude; the best time to visit is June to September for hiking and December to March for skiing. " +
                    "The Swiss public transport system, including trains, buses, and boats, is among the most efficient and scenic in the world.",
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/6/63/Matterhorn_from_Domh%C3%BCtte_-_2.jpg/800px-Matterhorn_from_Domh%C3%BCtte_-_2.jpg", 47.3769, 8.5417,
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
