package org.tphc.financeviews;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.context.annotation.Bean;

import org.springframework.data.repository.PagingAndSortingRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.util.ResourceUtils;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@SpringBootApplication
public class FinanceviewsApplication {

    private static final Logger log = LoggerFactory.getLogger(FinanceviewsApplication.class);

    public FinanceviewsApplication(StockTsRepository stockTsRepository, StockRepository stockRepository) {
        this.stockTsRepository = stockTsRepository;
        this.stockRepository = stockRepository;
    }


    public static void main(String[] args) {
        SpringApplication.run(FinanceviewsApplication.class, args);
    }

    private final StockTsRepository stockTsRepository;

    private final StockRepository stockRepository;

    private String uid() {
        return UUID.randomUUID().toString();
    }

    @Bean
    public CommandLineRunner demo() {
        return (args) -> {
//            final var c = new Company("Microsoft Corp.");
//            final var c2 = new Company("Apple Inc.");
//            companyRepository.saveAll(List.of(c, c2));
//            stocks.add(new Stock(c, "MSFT", UUID.randomUUID().toString(), UUID.randomUUID().toString()));
//            stocks.add(new Stock(c2, "AAPL", UUID.randomUUID().toString(), UUID.randomUUID().toString()));

            JacksonJsonParser jacksonJsonParser = new JacksonJsonParser();

            ObjectMapper objectMapper = new ObjectMapper();

            List<CompDump> listCompDump = objectMapper.readValue(ResourceUtils.getFile("classpath:comp_small.json")
                    , new TypeReference<List<CompDump>>() {
                    });

            listCompDump.parallelStream().map(this::mapDumpToStock)
                    .forEach(this::generateRandomStockData);

//            Stream.generate(this::generateRandomStock).limit(50)
//                    .forEach(this::generateRandomStockData);
        };

    }

    private Stock mapDumpToStock(CompDump dump) {
        var s = new Stock(dump.getCompanyName(), dump.getActSymbol(), uid(), uid());
        stockRepository.save(s);
        return s;
    }

    private Stock generateRandomStock() {
        var s = new Stock(uid(), uid(), uid(), uid());
        stockRepository.save(s);
        return s;
    }


    private List<StockTs> generateRandomStockData(Stock stock) {
        List<StockTs> stockTsList = new ArrayList<>();
        LocalDate dt = LocalDate.now();
        for (int i = 0; i < 100; i++) {
            dt = dt.plusDays(1);
            stockTsList.add(generateStockTsValue(stock, dt));
        }
        stockTsRepository.saveAll(stockTsList);
        return stockTsList;
    }

    private StockTs generateStockTsValue(Stock stock, LocalDate dt) {
        var s = new StockTs();
        s.setStock(stock);
        s.setDate(dt);
        s.setOpen(new BigDecimal(Math.random()).multiply(new BigDecimal("10")));
        s.setClose(s.getOpen().multiply(new BigDecimal("1.4")));
        return s;
    }

}

@Data
class CompDump {
    @JsonProperty(value = "ACT Symbol")
    private String actSymbol;
    @JsonProperty(value = "Company Name")
    private String companyName;
}

@RepositoryRestResource(collectionResourceRel = "stockTs", path = "data")
interface StockTsRepository extends PagingAndSortingRepository<StockTs, Long> {
    List<StockTs> findAllBy();
    List<StockTs> findByStockName(@Param("name") String name);
    List<StockTs> findByStockTicker(@Param("ticker") String ticker);
}

@RepositoryRestResource(collectionResourceRel = "stock", path = "stock")
interface StockRepository extends PagingAndSortingRepository<Stock, Long> {
    List<Stock> findByName(@Param("name") String name);
    List<Stock> findByTicker(@Param("ticker") String ticker);
}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@SequenceGenerator(name = "seq", allocationSize = 100)
class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq")
    private long id;

    @NonNull
    private String name;

    @NonNull
    private String ticker;

    @NonNull
    private String isin;

    @NonNull
    private String WKN;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "stock")
    private List<StockTs> stockTs;
}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@SequenceGenerator(name = "seq", allocationSize = 100)
class StockTs {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq")
    private long id;

    @NonNull
    @ManyToOne
    @JoinColumn
    private Stock stock;

    @NonNull
    private LocalDate date;

    @NonNull
    private BigDecimal open;

    @NonNull
    private BigDecimal close;
}
