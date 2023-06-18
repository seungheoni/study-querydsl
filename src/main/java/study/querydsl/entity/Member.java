package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id
    @GeneratedValue @Column(name = "member_id") private Long id;
    private String username; private int age;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id") private Team team;

    public Member(String username) {
        this(username, 0);
    }
    public Member(String username, int age) {
        this(username, age, null);
    }
    public Member(String username, int age, Team team) { this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    // 양방향 매핑시 사용되는 연관관계 편의 메서드 . 하나의 team 정보를 세팅할때 조인된 양쪽방향에서 넣어준다.
    public void changeTeam(Team team) { this.team = team; team.getMembers().add(this);
    }
}
