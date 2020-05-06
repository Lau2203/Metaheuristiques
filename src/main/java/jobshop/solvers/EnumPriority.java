package jobshop.solvers;

public enum EnumPriority {
    SPT, //(Shortest Processing Time) : donne priorité à la tâche la plus courte
    LPT, //(Longest Processing Time) : donne priorité à la tâche la plus longue
    SRPT, //(Shortest Remaining Processing Time) : donne la priorité à la tâche appartenant au jobayant la plus petite durée restante;
    LRPT, //(Longest Remaining Processing Time) : donne la priorité à la tâche appartenant au jobayant la plus grande durée
    EST_SPT, //SPT + restriction aux tâches pouvant commencer au plus tôt
    EST_LPT, //LPT + restriction aux tâches pouvant commencer au plus tôt
    EST_SRPT, //SRPT + restriction aux tâches pouvant commencer au plus tôt
    EST_LRPT //EST + restriction aux tâches pouvant commencer au plus tôt
}
