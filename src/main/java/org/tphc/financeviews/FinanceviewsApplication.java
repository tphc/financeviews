package org.tphc.financeviews;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.util.ResourceUtils;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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


    private void generateRandomStockData(Stock stock) {
        List<StockTs> stockTsList = new ArrayList<>();
        LocalDate dt = LocalDate.now();
        for (int i = 0; i < 100; i++) {
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

class CompDump {
    @JsonProperty(value = "ACT Symbol")
    private String actSymbol;
    @JsonProperty(value = "Company Name")
    private String companyName;

    public CompDump() {
    }

    public String getActSymbol() {
        return this.actSymbol;
    }

    public void setActSymbol(String actSymbol) {
        this.actSymbol = actSymbol;
    }

    public String getCompanyName() {
        return this.companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CompDump)) return false;
        final CompDump other = (CompDump) o;
        if (!other.canEqual(this)) return false;
        final Object this$actSymbol = this.getActSymbol();
        final Object other$actSymbol = other.getActSymbol();
        if (this$actSymbol == null ? other$actSymbol != null : !this$actSymbol.equals(other$actSymbol)) return false;
        final Object this$companyName = this.getCompanyName();
        final Object other$companyName = other.getCompanyName();
        return this$companyName == null ? other$companyName == null : this$companyName.equals(other$companyName);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CompDump;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $actSymbol = this.getActSymbol();
        result = result * PRIME + ($actSymbol == null ? 43 : $actSymbol.hashCode());
        final Object $companyName = this.getCompanyName();
        result = result * PRIME + ($companyName == null ? 43 : $companyName.hashCode());
        return result;
    }

    public String toString() {
        return "CompDump(actSymbol=" + this.getActSymbol() + ", companyName=" + this.getCompanyName() + ")";
    }
}

@Entity
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

    @java.beans.ConstructorProperties({"name", "ticker", "isin", "WKN"})
    public Stock(@NonNull String name, @NonNull String ticker, @NonNull String isin, @NonNull String WKN) {
        this.name = name;
        this.ticker = ticker;
        this.isin = isin;
        this.WKN = WKN;
    }

    public Stock() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return this.name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getTicker() {
        return this.ticker;
    }

    public void setTicker(@NonNull String ticker) {
        this.ticker = ticker;
    }

    @NonNull
    public String getIsin() {
        return this.isin;
    }

    public void setIsin(@NonNull String isin) {
        this.isin = isin;
    }

    @NonNull
    public String getWKN() {
        return this.WKN;
    }

    public void setWKN(@NonNull String WKN) {
        this.WKN = WKN;
    }

    public List<StockTs> getStockTs() {
        return this.stockTs;
    }

    public void setStockTs(List<StockTs> stockTs) {
        this.stockTs = stockTs;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Stock)) return false;
        final Stock other = (Stock) o;
        if (!other.canEqual(this)) return false;
        if (this.getId() != other.getId()) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$ticker = this.getTicker();
        final Object other$ticker = other.getTicker();
        if (this$ticker == null ? other$ticker != null : !this$ticker.equals(other$ticker)) return false;
        final Object this$isin = this.getIsin();
        final Object other$isin = other.getIsin();
        if (this$isin == null ? other$isin != null : !this$isin.equals(other$isin)) return false;
        final Object this$WKN = this.getWKN();
        final Object other$WKN = other.getWKN();
        if (this$WKN == null ? other$WKN != null : !this$WKN.equals(other$WKN)) return false;
        final Object this$stockTs = this.getStockTs();
        final Object other$stockTs = other.getStockTs();
        return this$stockTs == null ? other$stockTs == null : this$stockTs.equals(other$stockTs);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Stock;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $id = this.getId();
        result = result * PRIME + (int) ($id >>> 32 ^ $id);
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $ticker = this.getTicker();
        result = result * PRIME + ($ticker == null ? 43 : $ticker.hashCode());
        final Object $isin = this.getIsin();
        result = result * PRIME + ($isin == null ? 43 : $isin.hashCode());
        final Object $WKN = this.getWKN();
        result = result * PRIME + ($WKN == null ? 43 : $WKN.hashCode());
        final Object $stockTs = this.getStockTs();
        result = result * PRIME + ($stockTs == null ? 43 : $stockTs.hashCode());
        return result;
    }

    public String toString() {
        return "Stock(id=" + this.getId() + ", name=" + this.getName() + ", ticker=" + this.getTicker() + ", isin=" + this.getIsin() + ", WKN=" + this.getWKN() + ", stockTs=" + this.getStockTs() + ")";
    }
}

@Entity
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

    @java.beans.ConstructorProperties({"stock", "date", "open", "close"})
    public StockTs(@NonNull Stock stock, @NonNull LocalDate date, @NonNull BigDecimal open, @NonNull BigDecimal close) {
        this.stock = stock;
        this.date = date;
        this.open = open;
        this.close = close;
    }

    public StockTs() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public Stock getStock() {
        return this.stock;
    }

    public void setStock(@NonNull Stock stock) {
        this.stock = stock;
    }

    @NonNull
    public LocalDate getDate() {
        return this.date;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = date;
    }

    @NonNull
    public BigDecimal getOpen() {
        return this.open;
    }

    public void setOpen(@NonNull BigDecimal open) {
        this.open = open;
    }

    @NonNull
    public BigDecimal getClose() {
        return this.close;
    }

    public void setClose(@NonNull BigDecimal close) {
        this.close = close;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof StockTs)) return false;
        final StockTs other = (StockTs) o;
        if (!other.canEqual(this)) return false;
        if (this.getId() != other.getId()) return false;
        final Object this$stock = this.getStock();
        final Object other$stock = other.getStock();
        if (this$stock == null ? other$stock != null : !this$stock.equals(other$stock)) return false;
        final Object this$date = this.getDate();
        final Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        final Object this$open = this.getOpen();
        final Object other$open = other.getOpen();
        if (this$open == null ? other$open != null : !this$open.equals(other$open)) return false;
        final Object this$close = this.getClose();
        final Object other$close = other.getClose();
        return this$close == null ? other$close == null : this$close.equals(other$close);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof StockTs;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $id = this.getId();
        result = result * PRIME + (int) ($id >>> 32 ^ $id);
        final Object $stock = this.getStock();
        result = result * PRIME + ($stock == null ? 43 : $stock.hashCode());
        final Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        final Object $open = this.getOpen();
        result = result * PRIME + ($open == null ? 43 : $open.hashCode());
        final Object $close = this.getClose();
        result = result * PRIME + ($close == null ? 43 : $close.hashCode());
        return result;
    }

    public String toString() {
        return "StockTs(id=" + this.getId() + ", stock=" + this.getStock() + ", date=" + this.getDate() + ", open=" + this.getOpen() + ", close=" + this.getClose() + ")";
    }
}
