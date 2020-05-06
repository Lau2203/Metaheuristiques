package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class Gloutonne implements Solver {


    public static EnumPriority enumPriority;
    //Instance pour méthode solve
    private Instance instance;
    //Date de libération des machines
    private int[] libMachine;
    //Array de toutes les tâches qui n'ont pas encore été traitées par les machines -> while pasVide on continue
    private ArrayList<Task> tachesNonRealisees;
    //Durée restante pour les jobs, pour algo SRPT et LRPT
    private int[][] dureeRestante;


    //C'est le constructeur de la classe, il détermine si le choix des tâches se fait avec SPT, LPT, SRPT ou LRPT
    public Gloutonne(EnumPriority enumPriority) {
        this.enumPriority = enumPriority;
    }


    //C'est l'algorithme général de résolution, identique quelle que soit la priorité, il est hérité de l'interface Solver
    //Les priorités seront traitées dans une autre méthode qui ordonnera les tâches à traiter
    @Override
    public Result solve(Instance instance, long deadline) {

        //INSTANCIATION DU PROBLEME
        this.instance = instance;
        this.libMachine = new int[this.instance.numMachines];
        //remplissement avec des 0 du tableau précedemment créé
        for (int i = 0; i < libMachine.length; i++) {
            libMachine[i] = 0;
        }
        //création de l'array des tâches non-réalisées avec création des tâches correspondantes
        this.tachesNonRealisees = new ArrayList<Task>();
        for (int job = 0; job < this.instance.numJobs; job++) {
            this.tachesNonRealisees.add(new Task(job, 0));
        }

        //gestion des temps restants si SRPT ou LRPT
        if (this.enumPriority == EnumPriority.LRPT || this.enumPriority == EnumPriority.SRPT || this.enumPriority == EnumPriority.EST_LRPT || this.enumPriority == EnumPriority.EST_SRPT) {
            //initialisation matrice Job/Task
            dureeRestante = new int[this.instance.numJobs][this.instance.numTasks];
            //remplir le [][] avec des -1
            for (int job = 0; job < this.instance.numJobs; job++) {
                for (int task = 0; task < this.instance.numTasks; task++) {
                    dureeRestante[job][task] = -1;
                }
            }
        }

        JobNumbers solution = new JobNumbers(instance);

        //DEBUT ALHORITHME
        //Au premier tour, il y a l'ensemble des tâches réalisables à traiter
        //A chaque boucle, on met à jour tâchesEnCours
        //L'algorithme s'arrête lorsqu'il n'y a plus de tâches à traiter, donc que l'Array est vide
        while (!(this.tachesNonRealisees.isEmpty())) {
            Task opt = this.tacheOpti(this.tachesNonRealisees);

            //Inspiré de BasicSolver
            solution.jobs[solution.nextToSet++] = opt.job;

            this.libMachine[this.instance.machine(opt.job, opt.task)] += this.instance.duration(opt.job, opt.task);

            //MISE A JOUR
            this.tachesNonRealisees.remove(this.tachesNonRealisees.indexOf(opt));
            if (opt.task < this.instance.numTasks - 1) {
                // Prochaine tache est en attente
                this.tachesNonRealisees.add(new Task(opt.job, opt.task + 1));
            }
        }

        //Cf classe Result
        return new Result(instance, solution.toSchedule(), Result.ExitCause.Timeout);
    }


    //Différence de tri suivant la priorité
    private Task tacheOpti(ArrayList<Task> tachesNonRealisees) {

        ArrayList<Task> resultat = new ArrayList<Task>();
        Task tacheN1 = tachesNonRealisees.get(0);


        //SPT --  priorité à la tâche la plus courte
        if (this.enumPriority == EnumPriority.SPT) {
            for (int i = 1; i < tachesNonRealisees.size(); i++) {
                Task tacheCourante = tachesNonRealisees.get(i);
                //comparaison de la durée avec l'optimum (tacheN1)
                if (this.instance.duration(tacheN1.job, tacheN1.task) > this.instance.duration(tacheCourante.job, tacheCourante.task)) {
                    tacheN1 = tacheCourante;
                }
            }
        }


        //LPT -- priorité à la tâche la plus longue
        else if (this.enumPriority == EnumPriority.LPT) {
            for (int i = 1; i < tachesNonRealisees.size(); i++) {
                Task tacheCourante = tachesNonRealisees.get(i);
                //comparaison de la durée avec l'optimum (tacheN1)
                if (this.instance.duration(tacheN1.job, tacheN1.task) < this.instance.duration(tacheCourante.job, tacheCourante.task)) {
                    tacheN1 = tacheCourante;
                }
            }


        //SRPT - tâche appartenant au job ayant la plus petite durée restante
        } else if (this.enumPriority == EnumPriority.SRPT) {

                for (int i = 1; i < tachesNonRealisees.size(); i++) {
                    Task tacheCourante = tachesNonRealisees.get(i);

                    //Calcul temps pas encore fait mais initialisé dans solve()
                    if (this.dureeRestante[tacheCourante.job][tacheCourante.task] == -1) {
                        this.dureeRestante[tacheCourante.job][tacheCourante.task] = 0;
                        //Somme de toutes les tâches
                        for (int tache = tacheCourante.task; tache < this.instance.numTasks; tache++) {
                            this.dureeRestante[tacheCourante.job][tacheCourante.task] += this.instance.duration(tacheCourante.job, tache);
                        }
                    }

                    if (this.dureeRestante[tacheN1.job][tacheN1.task] > this.dureeRestante[tacheCourante.job][tacheCourante.task]) {
                        tacheN1 = tacheCourante;
                    }
                }


        } else if (this.enumPriority == EnumPriority.LRPT) {

                for (int i = 1; i < tachesNonRealisees.size(); i++) {
                    Task tacheCourante = tachesNonRealisees.get(i);

                    //Calcul temps pas encore fait mais initialisé dans solve()
                    if (this.dureeRestante[tacheCourante.job][tacheCourante.task] == -1) {
                        this.dureeRestante[tacheCourante.job][tacheCourante.task] = 0;
                        //Somme de toutes les tâches
                        for (int tache = tacheCourante.task; tache < this.instance.numTasks; tache++) {
                            this.dureeRestante[tacheCourante.job][tacheCourante.task] += this.instance.duration(tacheCourante.job, tache);
                        }
                    }

                    if (this.dureeRestante[tacheN1.job][tacheN1.task] < this.dureeRestante[tacheCourante.job][tacheCourante.task]) {
                        tacheN1 = tacheCourante;
                    }
                }

        } else if(this.enumPriority == EnumPriority.EST_SPT) {
            for (int i = 1; i < tachesNonRealisees.size(); i++) {
                Task tacheCourante = tachesNonRealisees.get(i);
                //rajout d'une condition de plus que SPT, on regarde la date de libération des maĉhines
                if (this.libMachine[this.instance.machine(tacheN1.job, tacheN1.task)] > this.libMachine[this.instance.machine(tacheCourante.job, tacheCourante.task)]) {
                    //comparaison de la durée avec l'optimum (tacheN1)
                    if (this.instance.duration(tacheN1.job, tacheN1.task) > this.instance.duration(tacheCourante.job, tacheCourante.task)) {
                        tacheN1 = tacheCourante;
                    }
                }
            }

        } else if (this.enumPriority == EnumPriority.EST_LPT) {
            for (int i = 1; i < tachesNonRealisees.size(); i++) {
                Task tacheCourante = tachesNonRealisees.get(i);
                //rajout d'une condition de plus que SPT, on regarde la date de libération des maĉhines
                if (this.libMachine[this.instance.machine(tacheN1.job, tacheN1.task)] > this.libMachine[this.instance.machine(tacheCourante.job, tacheCourante.task)]) {
                    //comparaison de la durée avec l'optimum (tacheN1)
                    if (this.instance.duration(tacheN1.job, tacheN1.task) < this.instance.duration(tacheCourante.job, tacheCourante.task)) {
                        tacheN1 = tacheCourante;
                    }
                }
            }

        } else if (this.enumPriority == EnumPriority.EST_SRPT) {

            for (int i = 1; i < tachesNonRealisees.size(); i++) {
                Task tacheCourante = tachesNonRealisees.get(i);

                //Calcul temps pas encore fait mais initialisé dans solve()
                if (this.dureeRestante[tacheCourante.job][tacheCourante.task] == -1) {
                    this.dureeRestante[tacheCourante.job][tacheCourante.task] = 0;
                    //Somme de toutes les tâches
                    for (int tache = tacheCourante.task; tache < this.instance.numTasks; tache++) {
                        this.dureeRestante[tacheCourante.job][tacheCourante.task] += this.instance.duration(tacheCourante.job, tache);
                    }
                }
                if (this.libMachine[this.instance.machine(tacheN1.job, tacheN1.task)] > this.libMachine[this.instance.machine(tacheCourante.job, tacheCourante.task)]) {
                    if (this.dureeRestante[tacheN1.job][tacheN1.task] > this.dureeRestante[tacheCourante.job][tacheCourante.task]) {
                        tacheN1 = tacheCourante;
                    }
                }
            }
        } else if (this.enumPriority == EnumPriority.EST_LRPT) {

            for (int i = 1; i < tachesNonRealisees.size(); i++) {
                Task tacheCourante = tachesNonRealisees.get(i);

                //Calcul temps pas encore fait mais initialisé dans solve()
                if (this.dureeRestante[tacheCourante.job][tacheCourante.task] == -1) {
                    this.dureeRestante[tacheCourante.job][tacheCourante.task] = 0;
                    //Somme de toutes les tâches
                    for (int tache = tacheCourante.task; tache < this.instance.numTasks; tache++) {
                        this.dureeRestante[tacheCourante.job][tacheCourante.task] += this.instance.duration(tacheCourante.job, tache);
                    }
                }
                if (this.libMachine[this.instance.machine(tacheN1.job, tacheN1.task)] > this.libMachine[this.instance.machine(tacheCourante.job, tacheCourante.task)]) {
                    if (this.dureeRestante[tacheN1.job][tacheN1.task] < this.dureeRestante[tacheCourante.job][tacheCourante.task]) {
                        tacheN1 = tacheCourante;
                    }
                }
            }
        }
        return tacheN1;
    }


}
