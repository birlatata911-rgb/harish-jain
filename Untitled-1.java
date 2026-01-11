anime-tracker/
├── src/main/java/com/example/animetracker
│   ├── AnimeTrackerApplication.java
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── AnimeController.java
│   │   └── WatchlistController.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Anime.java
│   │   ├── Watchlist.java
│   │   └── WatchStatus.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── AnimeRepository.java
│   │   └── WatchlistRepository.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── AnimeService.java
│   │   └── WatchlistService.java
│   └── dto/
│       └── AuthRequest.java
└── src/main/resources
    └── application.yml
@SpringBootApplication
public class AnimeTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnimeTrackerApplication.class, args);
    }
}
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    private LocalDateTime createdAt = LocalDateTime.now();

    // getters & setters
}
@Entity
public class Anime {

    @Id
    private Long id; // AniList ID

    private String title;
    private String coverImage;
    private Integer episodes;
    private String status;

    // getters & setters
}
public enum WatchStatus {
    WATCHING,
    COMPLETED,
    ON_HOLD,
    DROPPED,
    PLAN_TO_WATCH
}
@Entity
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Anime anime;

    @Enumerated(EnumType.STRING)
    private WatchStatus status;

    private int currentEpisode;
    private int rating;

    @Column(length = 1000)
    private String notes;

    // getters & setters
}
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
public interface AnimeRepository extends JpaRepository<Anime, Long> {
}
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUserId(Long userId);
}
public class AuthRequest {
    public String email;
    public String password;
}
@Service
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public User register(AuthRequest request) {
        User user = new User();
        user.setEmail(request.email);
        user.setPassword(encoder.encode(request.password));
        return repo.save(user);
    }
}
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public User register(@RequestBody AuthRequest request) {
        return service.register(request);
    }
}
@Service
public class AnimeService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String searchAnime(String query) {

        String graphqlQuery = """
        {
          Media(search: "%s", type: ANIME) {
            id
            title { romaji }
            episodes
            coverImage { large }
            status
          }
        }
        """.formatted(query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("query", graphqlQuery);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(
                "https://graphql.anilist.co",
                entity,
                String.class
        );
    }
}
@RestController
@RequestMapping("/anime")
public class AnimeController {

    private final AnimeService service;

    public AnimeController(AnimeService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public String search(@RequestParam String q) {
        return service.searchAnime(q);
    }
}
@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistRepository repo;

    public WatchlistController(WatchlistRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{userId}")
    public List<Watchlist> getUserWatchlist(@PathVariable Long userId) {
        return repo.findByUserId(userId);
    }
}
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/animetracker
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    