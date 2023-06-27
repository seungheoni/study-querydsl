package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    @DisplayName("초창기 jpql 쿼리")
    public void startJPQL() {
        //jpql은 쿼리 오타가 검증이 안됨. 런타임에서 검증된다.
        String qlString = "select m from Members m " +
                          "where m.username = :username";
        Member findMember = em.createQuery(qlString,Member.class)
                .setParameter("username","member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("테이블 alias 적용하는 법, 조인할떄 많이 쓴다고함")
    public void startQuerydsl() {
        QMember m = new QMember("m");

        //jpql과 다르게 파라미터 바인딩을 해준다. 또한 쿼리 검증을 컴파일 타임에서 가능하다.
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("Q클래스 static import 방법")
    public void startQuerydsl2() {

        //아래처럼 Q클래스를 static import하여 사용하는걸 권장한다.
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("WHERE 자겅시 쿼리처럼 작성")
    public void search() {

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10,30)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName(",(콤마) 형태 방법으로 and안쓰고 where 작성도 가능. 김영한은 이걸 추천함.")
    public void searchAndParam() {

        //
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("fetch")
    public void resultFetch() {

        // fetch는 limit하고 다르다. 쿼리 질의 결과에서 가져오는걸 설정하는게 fetch

        //결과 리스트 가져옴 ,전체
        queryFactory.selectFrom(member)
                .fetch();

        // 결과 하나만 가져옴
        Member fetchOne = queryFactory.selectFrom(member)
                .fetchOne();

        Member fetchFrist = queryFactory.selectFrom(member)
                .fetchFirst();

        //페이징용 정보를 포함, total count 쿼리 추가 실행, 중요한곳에서는 fetchCount, fetchResults 둘다 안쓰고 직접 카운트한다고 함, deprecate예정
        QueryResults<Member> results = queryFactory.selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        // count수 조회 , 위와 같은 이유
        long otal = queryFactory.selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원나이 내림 차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    @DisplayName("정렬")
    public void sort() {
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    @DisplayName("페이징")
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);

    }

    @Test
    @DisplayName("집계 함수")
    public void aggregateion() {
        List<Tuple> result = queryFactory.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팅의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    @DisplayName("조인")
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username") //username이
                .containsExactly("member1","member2"); // member1과 member2을 포함하고 있는지
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    @DisplayName("세타 조인")
    public void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name)).fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 연관 관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    @DisplayName("세타 조인")
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple: result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("패치 조인 미 사용시")
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //해당 엔티티가 영속성 컨텍스트로 로딩된 엔티티 인지 아닌지를 판단해준다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    @DisplayName("패치 조인 사용시")
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //해당 엔티티가 영속성 컨텍스트로 로딩된 엔티티 인지 아닌지를 판단해준다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    @DisplayName("서브 쿼리는 JPAExpressions를 사용하면 됨")
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq
                        (JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
        )).fetch();
    }

    ////////////////////////// projection /////////////////////////////

    @Test
    @DisplayName("프로젝션 대상 한개일떄")
    public void simpleProjection() {
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    @DisplayName("일반적인 jpql을 사용할때의 프로젝션, new 오퍼레이션을 사용해서 직접 풀패키지경로의 생성자를 호출해야함")
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m ", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("querydsl 프로젝션시 튜플을 사용하는 방법")
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String usernmae = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + usernmae);
            System.out.println("age = " + age);
        }
    }

    @Test
    @DisplayName("1. querydsl 프로젝션 - Projections.bean 메서드 사용")
    public void findDtoByBean() {

        // bean메서드 사용시 dto의 기본 생성자와 프로젝션할 필드의 setter가 필요함. (기본 생성자를 만들어서 setter로 값을 세팅한다고함)"
        List<MemberDto> fetch = queryFactory.select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("2. querydsl 프로젝션 - Projections.fields 메서드 사용")
    public void findDtoByFields() {

        //fields 메서드를 사용하면 기본 생성자만 있고 getter만 있어도 됨, 내부에서 직접 필드에 꽂는다고 함.
        //내부에서 FieldProjection이라는 클래스를 생성해서 클래스와 필드이름 배열을 인자로 받아서 이걸 통해 프로젝션한다고함
        List<MemberDto> fetch = queryFactory.select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("3. querydsl 프로젝션 - Projections.field 메서드 사용시 dto 필드 이름이 다를떄 as 사용")
    public void findDtoByFieldOtherFieldNameAS() {

        //위의 이유로 field 사용시 필드 이름이 같아야함. 따라서 필드 이름이 다르다고 하면 as를 통해 별칭을 줄 수 있음
        List<UserDto> fetch = queryFactory.select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }
    @Test
    @DisplayName("4. querydsl 프로젝션 - Projections.field 메서드 사용시 dto 필드 이름이 다를떄 ExpressionUtil 사용")
    public void findDtoByFieldOtherFieldNameExpressionUtil() {

        QMember memberSub = new QMember("memberSub");

        // 필드 이름이 다르다고 하면 ExpressionUtils를 통해 별칭을 줄 수 있고 해당 방법은 내부에 서브쿼리를 통해서 추가적인 작업도 가능하다
        List<UserDto> fetch = queryFactory.select(Projections.fields(UserDto.class,
                        ExpressionUtils.as(member.username,"name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub),"age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }
    @Test
    @DisplayName("5. querydsl 프로젝션 - Projections.constructor 메서드 사용")
    public void findDtoByConstructor() {

        // dto에 생성자를 사용하는 constructor, 단 프로젝션 순서와 dto 생성자 파라미터 순서를 맞춰야한다. 프로젝션에 맞는 생성자가 있어야한다.
        List<MemberDto> fetch = queryFactory.select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("7. querydsl 프로젝션 - Projections.constructor 컴파일 단계에서 검사를 해주지 않음")
    public void findDtoByConstructorError() {

        // constructor 메서드는 컴파일 단계에서 생성자에 대한 타입 검사를 해주지 않는다.. 아래와 같이 id 필드가 생겨버리면 런타임에서 에러가 발생
        List<UserDto> fetch = queryFactory.select(Projections.fields(UserDto.class,
                        member.username,
                        member.age,
                        member.id))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    @DisplayName("8.dto생성자에 @QueryProjection 어노테이션을 활용한 방법, 컴파일 단계에서 검사를 해준다. ")
    public void findDtoByQueryProjection() {

        //@QueryProjection을 사용하여 컴파일된 q dto 클래스의 생성자는 알아서 타입을 컴파일 단계에서 타입이나 파라미터 검사해준다. 실제 dto 생성자를 호출함
        //cmd + p 로 파라미터 순서도 확인가능

        // 단점이 따로 1. dto도 컴파일을 해야함.
        // 2. dto는 의존성이 없어야 좋은데., dto가 QueryProjection 이라는 어노테이션에 의존성이 생겨버림,
        List<MemberDto> fetch = queryFactory.select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();


        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    ////////////////////////////////////////  동적 쿼리 ////////////////////////////////////////
    @Test
    @DisplayName("동적쿼리 - BooleanBuilder를 활용한 동적 쿼리 작성 방법.")
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List <Member> result = searchMember1(usernameParam,ageParam);

        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory.selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    @DisplayName("동적쿼리 - .where의 null 활용한 동적 쿼리 작성 방법. 쿼리 조립에 대한 장점")
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List <Member> result = searchMember2(usernameParam,ageParam);

        assertThat(result.size()).isEqualTo(1);
    }


    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        // where 절에 null 파라미터가 들어가면 쿼리를 만들지 않는걸 이용.
        return queryFactory.selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond == null ? null : member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    public BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    //////////////// 수정 삭제 벌크 //////////////////

    @Test
    @Commit
    @DisplayName("벌크 update 연산, 그리고 벌크 주의사항")
    public void bulkUpdate() {

        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원
        //member3 = 30 -> 유지
        //member4 = 40 -> 유지

        // 해당 업데이트 쿼리는 영속성 컨텍스트를 무시하고 db에서 업데이트를 친다.
        long cound = queryFactory
                .update(member)
                .set(member.username,"비회원")
                .where(member.age.lt(30))
                .execute();

        //영속성 컨텍스트와 맞추기위해서 벌크 연산후 영속성 컨텍스트를 초기화 한다.
        em.flush();
        em.clear();

        //주의 . flush, clear를 안할경우
        //영속성컨텍스트가 우선권이 있어서 db에서 값이 변경되서 조회를 다시 하더라도. 영속성 컨텍스트에 이미 존재하고 있다면 db조회 값을 버려버림
        List<Member> result = queryFactory.selectFrom(member).fetch();

        //해당 결과값이 db와 다르다는 것을 알 수 잇음
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

    }

    @Test
    @DisplayName("벌크 add 연산")
    public void bulkAdd() {
        long count = queryFactory.update(member)
                .set(member.age,member.age.add(1))
                .execute();
    }

    @Test
    @DisplayName("벌크 delete 연산")
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }
}
