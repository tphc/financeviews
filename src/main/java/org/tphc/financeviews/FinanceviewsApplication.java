package org.tphc.financeviews;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
@RepositoryRestResource(collectionResourceRel = "stockTs", path = "data")
interface StockTsRepository extends PagingAndSortingRepository<StockTs, Long> {
    List<StockTs> findAllBy();

    List<StockTs> findByStockName(@Param("name") String name);

    List<StockTs> findByStockTicker(@Param("ticker") String ticker);
}

@SuppressWarnings("unused")
@RepositoryRestResource(collectionResourceRel = "stock", path = "stock")
interface StockRepository extends PagingAndSortingRepository<Stock, Long> {
    List<Stock> findByName(@Param("name") String name);

    List<Stock> findByTicker(@Param("ticker") String ticker);
}

@SpringBootApplication
public class FinanceviewsApplication {

    private static final Logger log = LoggerFactory.getLogger(FinanceviewsApplication.class);

    private final StockTsRepository stockTsRepository;
    private final StockRepository stockRepository;

    public FinanceviewsApplication(StockTsRepository stockTsRepository, StockRepository stockRepository) {
        this.stockTsRepository = stockTsRepository;
        this.stockRepository = stockRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(FinanceviewsApplication.class, args);
    }

    private String uid() {
        return UUID.randomUUID().toString();
    }

    @Bean
    public CommandLineRunner demo() {
        return (args) -> {
            log.info("startdemo");

            ObjectMapper objectMapper = new ObjectMapper();
            long start = System.nanoTime();
            log.info("start parsing: " + start);
            List<CompDump> listCompDump = objectMapper.readValue(this.getClass().getClassLoader().getResourceAsStream("comp.json")
                    , new TypeReference<List<CompDump>>() {
                    });

            long parsed = System.nanoTime();
            log.info("parsed in : " + (parsed -start)/ 1000000 + "ms");
            log.info("start inserting: " + System.nanoTime());
            listCompDump.parallelStream().map(this::mapDumpToStock)
                    .forEach(this::generateRandomStockData);
            log.info("inserted in : " + (System.nanoTime() -parsed)/ 1000000 + "ms");

        };

    }

    private Stock mapDumpToStock(CompDump dump) {
        var s = new Stock(dump.getCompanyName(), dump.getActSymbol(), uid(), uid());
        stockRepository.save(s);
        return s;
    }


    private void generateRandomStockData(Stock stock) {
        List<StockTs> stockTsList = new ArrayList<>();
        LocalDate dt = LocalDate.now();
        for (int i = 0; i <= 1000; i++) {
            dt = dt.plusDays(1);
            stockTsList.add(generateStockTsValue(stock, dt));
        }
        stockTsRepository.saveAll(stockTsList);
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

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor

class Stock {

    @Id
    @SequenceGenerator(name = "seq")
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

    @Column
    @CreationTimestamp
    private LocalDateTime createDateTime;

    @Column
    @UpdateTimestamp
    private LocalDateTime updateDateTime;
}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@SequenceGenerator(name = "seq")
class StockTs {

    @Id
    @SequenceGenerator(name = "seq")
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

    @Column
    @CreationTimestamp
    private LocalDateTime createDateTime;

    @Column
    @UpdateTimestamp
    private LocalDateTime updateDateTime;
}
