package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.DescentSolver.*;

import java.util.ArrayList;
import java.util.List;

import static jobshop.solvers.DescentSolver.blocksOfCriticalPath;

public class Tabou extends DescentSolver {

    @Override
    public Result solve(Instance instance, long deadline) {

        int[][] solutionTaboo = new int[instance.numTasks*instance.numJobs][instance.numJobs*instance.numTasks] ;
        int nombreIter = 0 ;
        int maxIter = 50 ;
        int dureeTaboo = 2 ;

        //Schedule bestSolution = new Gloutonne(Gloutonne.enumPriority.EST_LRPT).solve(instance, deadline).schedule;
        Schedule bestSolution = new DescentSolver().solve(instance, deadline).schedule;


        ResourceOrder currentResourceOrder= new ResourceOrder(bestSolution) ;
        ResourceOrder bestResourceOrder = currentResourceOrder.copy() ;

        int meilleurMakespan;

        while((nombreIter <= maxIter) && (deadline - System.currentTimeMillis() > 1)) {
            nombreIter++ ;

            Task t1;
            Task t2;
            List<Task> listTask = currentResourceOrder.toSchedule().criticalPath() ;
            List<Swap> listeSwap = new ArrayList<>() ;
            List<Block> listBlock = blocksOfCriticalPath(currentResourceOrder) ;
            for(int block = 0; block < listBlock.size(); block++) {
                listeSwap.addAll(neighbors(listBlock.get(block))) ;
            }

            //initialisation
            int indexBestSwap = -1;
            //bestSpan = 99999999;
            meilleurMakespan = Integer.MAX_VALUE ;

            for(int swap = 0; swap < listeSwap.size(); swap++) {
                ResourceOrder testResourceOrder = currentResourceOrder.copy() ;
                Swap currentSwap = listeSwap.get(swap) ;

                if (currentSwap.t1 < listTask.size()){
                    t1 = listTask.get(currentSwap.t1);
                } else { //NullPointer sinon
                    t1 = null;
                }


                if (currentSwap.t2 < listTask.size()){
                    t2 = listTask.get(currentSwap.t2);
                } else { //NullPointer sinon
                    t2 = null;
                }

                currentSwap.applyOn(testResourceOrder) ;

                //on regarde si le résultat est améliorant
                if ((t1 == null) || (t2 == null)){ //NullPointer sinon
                    break;
                } else if ((testResourceOrder.toSchedule().makespan() < meilleurMakespan) && (nombreIter >= solutionTaboo[t1.job * instance.numTasks + t1.task][t2.job * instance.numTasks + t2.task])) {
                        meilleurMakespan = testResourceOrder.toSchedule().makespan();
                        bestResourceOrder = testResourceOrder.copy();
                        indexBestSwap = swap;
                    }
                }


            if  (indexBestSwap != -1) {
                currentResourceOrder = bestResourceOrder.copy();
                //fin de parcours, on récupère le meilleur
                Swap bestSwap = listeSwap.get(indexBestSwap);
                t1 = listTask.get(bestSwap.t1);
                t2 = listTask.get(bestSwap.t2);
                //ajout dans matrice
                solutionTaboo[t2.job * instance.numTasks + t2.task][t1.job * instance.numTasks + t1.task] = nombreIter + dureeTaboo;
            }

        }
        return new Result(instance, bestResourceOrder.toSchedule(), Result.ExitCause.Timeout) ;
    }
}

