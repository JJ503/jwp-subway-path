package subway.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import subway.dao.entity.LineEntity;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@JdbcTest
@Sql("classpath:initializeTestDb.sql")
class LineDaoTest {

    private RowMapper<LineEntity> rowMapper = (rs, rowNum) ->
            new LineEntity(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("color")
            );
    private List<LineEntity> expectLines = List.of(
            new LineEntity(1L, "2호선", "초록색"),
            new LineEntity(2L, "1호선", "파랑색")
    );

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    DataSource dataSource;
    private LineDao lineDao;

    @BeforeEach
    void setup() {
        lineDao = new LineDao(jdbcTemplate, dataSource);
    }

    @DisplayName("노선을 추가한다")
    @Test
    void insert() {
        LineEntity insertLine = lineDao.insert(new LineEntity("4호선", "bg-yellow-600"));
        LineEntity line = jdbcTemplate.queryForObject(
                "SELECT id, name, color FROM line WHERE id = :id",
                new MapSqlParameterSource("id", insertLine.getId()),
                rowMapper
        );
        assertThat(line).usingRecursiveComparison()
                              .isEqualTo(insertLine);
    }

    @DisplayName("이미 존재하는 노선을 추가하면 예외가 발생한다")
    @Test
    void insertException() {
        assertThatThrownBy(() ->
                lineDao.insert(new LineEntity("2호선", "초록색"))
        ).isInstanceOf(DuplicateKeyException.class);
    }

    @DisplayName("전체 노선 목록을 조회한다")
    @Test
    void findAll() {
        List<LineEntity> findLines = lineDao.findAll();
        assertThat(findLines).usingRecursiveComparison()
                             .isEqualTo(expectLines);
    }

    @DisplayName("특정 노선을 조회하면 해당 노선의 엔티티가 반환된다")
    @ParameterizedTest(name = "{0}번 아이를 가진 노선 조회")
    @ValueSource(longs = {1L, 2L})
    void findById(Long findId) {
        Optional<LineEntity> findLine = lineDao.findById(findId);
        int findIndex = (int) (findId - 1);
        assertThat(findLine.get()).usingRecursiveComparison()
                                  .isEqualTo(expectLines.get(findIndex));
    }

    @DisplayName("존재하지 않는 아이디를 조회하면 빈 값을 반환한다")
    @Test
    void findByIdNotExist() {
        Long notExistId = 1000L;
        Optional<LineEntity> findLine = lineDao.findById(notExistId);
        assertThat(findLine).isEmpty();
    }

    @DisplayName("이름으로 특정 노선을 조회할 수 있다")
    @ParameterizedTest(name = "이름이 {0}인 노선 조회")
    @ValueSource(strings = {"2호선", "1호선"})
    void findByName(String name) {
        Optional<LineEntity> findLine = lineDao.findByName(name);

        assertAll(
                () -> assertThat(findLine.get()).isNotNull(),
                () -> assertThat(findLine.get().getName()).isEqualTo(name)
        );
    }

    @DisplayName("존재하지 않는 이름으로 노선 조회시 빈 값이 반환된다")
    @Test
    void findByNameNotExist() {
        String notExistName = "존재하지 않음";

        Optional<LineEntity> findLine = lineDao.findByName(notExistName);

        assertThat(findLine).isEmpty();
    }


    @DisplayName("색으로 특정 노선을 조회할 수 있다")
    @ParameterizedTest(name = "색이 {0}인 노선 조회")
    @ValueSource(strings = {"초록색", "파랑색"})
    void findByColor(String color) {
        Optional<LineEntity> findLine = lineDao.findByColor(color);

        assertAll(
                () -> assertThat(findLine.get()).isNotNull(),
                () -> assertThat(findLine.get().getColor()).isEqualTo(color)
        );
    }


    @DisplayName("존재하지 않는 이름으로 노선 조회시 빈 값이 반환된다")
    @Test
    void findByColorNotExist() {
        String notExistColor = "존재하지 않음";

        Optional<LineEntity> findLine = lineDao.findByColor(notExistColor);

        assertThat(findLine).isEmpty();
    }


    @DisplayName("특정 노선의 정보를 수정할 수 있다")
    @Test
    void update() {
        LineEntity updateEntity = new LineEntity(1L, "2호선", "노랑색");
        lineDao.update(updateEntity);
        LineEntity lineEntity = jdbcTemplate.queryForObject(
                "SELECT id, name, color FROM line WHERE id = :id",
                new BeanPropertySqlParameterSource(updateEntity),
                rowMapper
        );
        assertThat(lineEntity).usingRecursiveComparison()
                              .isEqualTo(updateEntity);
    }

    @DisplayName("특정 노선의 정보를 수정 시 이미 존재하는 이름 혹은 색인 경우 예외를 반환한다")
    @Test
    void updateException() {
        assertThatThrownBy(() ->
                lineDao.update(new LineEntity(1L, "2호선", "파랑색"))
        ).isInstanceOf(DuplicateKeyException.class);
    }

    @DisplayName("아이디로 특정 노선을 삭제할 수 있다")
    @Test
    void deleteById() {
        LineEntity deleteLine = jdbcTemplate.queryForObject(
                "SELECT * FROM line WHERE id = :id",
                new MapSqlParameterSource("id", 1L),
                rowMapper
        );

        lineDao.deleteById(deleteLine.getId());

        List<LineEntity> lineEntities = jdbcTemplate.query(
                "SELECT * FROM line",
                rowMapper
        );
        assertThat(lineEntities).usingRecursiveFieldByFieldElementComparator()
                                .doesNotContain(deleteLine);
    }
}
